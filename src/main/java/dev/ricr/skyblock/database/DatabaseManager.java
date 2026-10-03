package dev.ricr.skyblock.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.table.TableUtils;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.utils.Tuple;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Getter
public class DatabaseManager {
    private final SimpleSkyblock plugin;
    private final DatabaseChangesAccumulator accumulator;
    private ConnectionSource connection = null;

    private Dao<MigrationEntity, String> migrationsDao;
    private Dao<IslandPlayerTrustLinkEntity, String> islandPlayerTrustLinksDao;
    private Dao<PlayerEntity, String> playersDao;
    private Dao<IslandEntity, String> islandsDao;
    private Dao<GambleEntity, Integer> gamblesDao;
    private Dao<AuctionHouseItemEntity, Integer> auctionHouseDao;
    private Dao<TransactionEntity, Integer> transactionsDao;
    private Dao<WarpEntity, String> warpsDao;
    private Dao<VillagerShopEntity, String> villagerShopsDao;
    private Dao<IslandBlockedPlayersEntity, String> islandBlockedPlayersDao;

    public DatabaseManager(SimpleSkyblock plugin, DatabaseChangesAccumulator accumulator) {
        this.plugin = plugin;
        this.accumulator = accumulator;
        File dataFolder = plugin.getDataFolder();
        String databaseUrl = String.format("jdbc:sqlite:%s/%s", dataFolder.getAbsolutePath(), "database.sql");

        plugin.getLogger()
                .info("Connecting to database in " + databaseUrl);

        try {
            this.connection = new JdbcConnectionSource(databaseUrl);

            this.migrationsDao = DaoManager.createDao(this.connection, MigrationEntity.class);
            this.islandPlayerTrustLinksDao = DaoManager.createDao(this.connection, IslandPlayerTrustLinkEntity.class);
            this.playersDao = DaoManager.createDao(this.connection, PlayerEntity.class);
            this.islandsDao = DaoManager.createDao(this.connection, IslandEntity.class);
            this.gamblesDao = DaoManager.createDao(this.connection, GambleEntity.class);
            this.auctionHouseDao = DaoManager.createDao(this.connection, AuctionHouseItemEntity.class);
            this.transactionsDao = DaoManager.createDao(this.connection, TransactionEntity.class);
            this.warpsDao = DaoManager.createDao(this.connection, WarpEntity.class);
            this.villagerShopsDao = DaoManager.createDao(this.connection, VillagerShopEntity.class);
            this.islandBlockedPlayersDao = DaoManager.createDao(this.connection, IslandBlockedPlayersEntity.class);

            TableUtils.createTableIfNotExists(this.connection, MigrationEntity.class);
            TableUtils.createTableIfNotExists(this.connection, IslandPlayerTrustLinkEntity.class);
            TableUtils.createTableIfNotExists(this.connection, PlayerEntity.class);
            TableUtils.createTableIfNotExists(this.connection, IslandEntity.class);
            TableUtils.createTableIfNotExists(this.connection, TransactionEntity.class);
            TableUtils.createTableIfNotExists(this.connection, GambleEntity.class);
            TableUtils.createTableIfNotExists(this.connection, AuctionHouseItemEntity.class);
            TableUtils.createTableIfNotExists(this.connection, TransactionEntity.class);
            TableUtils.createTableIfNotExists(this.connection, WarpEntity.class);
            TableUtils.createTableIfNotExists(this.connection, VillagerShopEntity.class);
            TableUtils.createTableIfNotExists(this.connection, IslandBlockedPlayersEntity.class);

            plugin.getLogger()
                    .info("Successfully connected to database.");

            this.scheduleDbCommitTask(this.connection);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
        }
    }

    public void scheduleDbCommitTask(ConnectionSource connectionSource) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, () -> {
            var changes = this.accumulator.drain();
            if (changes.isEmpty()) {
                return;
            }

            try {
                DatabaseConnection connection = connectionSource.getReadWriteConnection(null);

                try {
                    connection.setAutoCommit(false);

                    for (DatabaseChange change : changes) {
                        this.applyChange(change);
                    }

                    connection.commit(null);
                } catch (Exception e) {
                    connection.rollback(null);
                    throw e;
                } finally {
                    connectionSource.releaseConnection(connection);
                }

            } catch (Exception e) {
                this.plugin.getLogger().severe("Failed to commit DB changes:");
                this.plugin.getLogger().severe(e.getMessage());
            }

            this.plugin.getLogger().info("Committed DB changes");
        }, 20L, 20L * 5); // 20 ticks per second * 5 seconds
    }

    public void commitImmediately() throws SQLException {
        var changes = this.accumulator.drain();
        if (changes.isEmpty()) {
            return;
        }

        for (DatabaseChange change : changes) {
            this.applyChange(change);
        }
    }

    public void runMigrations(File pluginFile) throws IOException {
        var jarsTuple = this.loadMigrationEntries(pluginFile);

        for (var migrationJarEntry : jarsTuple.getSecond()) {
            var filename = migrationJarEntry.getName();
            var migrationId = filename.substring("migrations/".length()).split("_")[0];

            MigrationEntity migrationEntity;
            try {
                migrationEntity = this.migrationsDao.queryForId(migrationId);
            } catch (SQLException e) {
                this.plugin.getLogger().severe(
                        String.format("Failed when querying migration %s using id %s: %s", filename, migrationId, e.getMessage())
                );
                continue;
            }

            if (migrationEntity != null) {
                this.plugin.getLogger().info(String.format("Skipping migration %s because it has been executed before", filename));
                continue;
            }

            this.plugin.getLogger().info(String.format("Executing new migration %s", filename));

            migrationEntity = new MigrationEntity();
            migrationEntity.setId(migrationId);
            migrationEntity.setMigration(filename.substring("migrations/".length()));

            String sql;
            try (InputStream in = jarsTuple.getFirst().getInputStream(migrationJarEntry)) {
                sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                this.plugin.getLogger().severe(
                        String.format("Failed when trying to parse the raw sql for migration %s: %s", filename, e.getMessage())
                );
                continue;
            }

            try {
                var rawConnection = this.connection.getReadWriteConnection(null);
                rawConnection.executeStatement(sql, DatabaseConnection.DEFAULT_RESULT_FLAGS);

                this.migrationsDao.create(migrationEntity);
            } catch (SQLException e) {
                this.plugin.getLogger().severe(
                        String.format("Failed when creating a new entry for migration %s with id %s: %s",
                                filename, migrationId, e.getMessage())
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        jarsTuple.getFirst().close();
    }

    private Tuple<JarFile, List<JarEntry>> loadMigrationEntries(File pluginFile) {
        try {
            JarFile jar = new JarFile(pluginFile);
            return new Tuple<>(jar, jar.stream()
                    .filter(e -> e.getName().startsWith("migrations/"))
                    .filter(e -> !e.isDirectory())
                    .sorted(Comparator.comparing(JarEntry::getName))
                    .toList());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read migrations from JAR", e);
        }
    }

    private void applyChange(DatabaseChange change) throws SQLException {
        switch (change) {
            case DatabaseChange.PlayerCreateOrUpdate(PlayerEntity player) -> {
                this.playersDao.createOrUpdate(player);

                var playerId = UUID.fromString(player.getPlayerId());

                var playerFastBoard = this.plugin.onlinePlayers.getPlayer(playerId).getFastBoard();
                if (playerFastBoard != null) {
                    playerFastBoard.updateMoney();
                }
            }
            case DatabaseChange.GambleRecordAdd(GambleEntity gamble) -> this.gamblesDao.create(gamble);
            case DatabaseChange.AuctionHouseItemAdd(AuctionHouseItemEntity auctionHouseItem) ->
                    this.auctionHouseDao.create(auctionHouseItem);
            case DatabaseChange.AuctionHouseItemRemove(AuctionHouseItemEntity auctionHouseItem) ->
                    this.auctionHouseDao.delete(auctionHouseItem);
            case DatabaseChange.IslandRecordUpdate(IslandEntity islandEntity) -> this.islandsDao.update(islandEntity);
            case DatabaseChange.BlockedPlayerAdd(IslandEntity playerIsland, PlayerEntity targetPlayer) -> {
                var blockedPlayerIslandLink = new IslandBlockedPlayersEntity();

                blockedPlayerIslandLink.setIsland(playerIsland);
                blockedPlayerIslandLink.setPlayer(targetPlayer);

                this.islandBlockedPlayersDao.create(blockedPlayerIslandLink);
            }
            case DatabaseChange.BlockedPlayerRemove(String islandOwnerId, String trustedPlayerId) -> {
                var deleteBuilder = this.plugin.databaseManager.getIslandBlockedPlayersDao().deleteBuilder();
                deleteBuilder.where()
                        .eq("island_id", islandOwnerId)
                        .and()
                        .eq("player_id", trustedPlayerId);
                deleteBuilder.delete();
            }
            case DatabaseChange.TransactionAdd(TransactionEntity transaction) ->
                    this.transactionsDao.create(transaction);
            case DatabaseChange.TrustedPlayerAdd(IslandEntity playerIsland, PlayerEntity targetPlayer) -> {
                var islandPlayerTrustLink = new IslandPlayerTrustLinkEntity();

                islandPlayerTrustLink.setIsland(playerIsland);
                islandPlayerTrustLink.setPlayer(targetPlayer);

                this.islandPlayerTrustLinksDao.create(islandPlayerTrustLink);
            }
            case DatabaseChange.TrustedPlayerRemove(String islandOwnerId, String trustedPlayerId) -> {
                var deleteBuilder = this.plugin.databaseManager.getIslandPlayerTrustLinksDao().deleteBuilder();
                deleteBuilder.where()
                        .eq("island_id", islandOwnerId)
                        .and()
                        .eq("player_id", trustedPlayerId);
                deleteBuilder.delete();
            }
            case DatabaseChange.WarpEntityCreateOrUpdate(WarpEntity warpEntity) ->
                    this.warpsDao.createOrUpdate(warpEntity);
            default -> throw new IllegalStateException("Unexpected value: " + change);
        }
    }
}
