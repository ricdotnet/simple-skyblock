package dev.ricr.skyblock.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.table.TableUtils;
import dev.ricr.skyblock.SimpleSkyblock;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.SQLException;

@Getter
public class DatabaseManager {
    private final SimpleSkyblock plugin;
    private final DatabaseChangesAccumulator accumulator;

    private Dao<IslandPlayerTrustLinkEntity, String> islandPlayerTrustLinksDao;
    private Dao<PlayerEntity, String> playersDao;
    private Dao<IslandEntity, String> islandsDao;
    private Dao<GambleEntity, Integer> gamblesDao;
    private Dao<AuctionHouseItemEntity, Integer> auctionHouseDao;
    private Dao<TransactionEntity, Integer> transactionsDao;

    public DatabaseManager(SimpleSkyblock plugin, DatabaseChangesAccumulator accumulator) {
        this.plugin = plugin;
        this.accumulator = accumulator;
        File dataFolder = plugin.getDataFolder();
        String databaseUrl = String.format("jdbc:sqlite:%s/%s", dataFolder.getAbsolutePath(), "database.sql");

        plugin.getLogger()
                .info("Connecting to database in " + databaseUrl);

        try {
            ConnectionSource connection = new JdbcConnectionSource(databaseUrl);

            this.islandPlayerTrustLinksDao = DaoManager.createDao(connection, IslandPlayerTrustLinkEntity.class);
            this.playersDao = DaoManager.createDao(connection, PlayerEntity.class);
            this.islandsDao = DaoManager.createDao(connection, IslandEntity.class);
            this.gamblesDao = DaoManager.createDao(connection, GambleEntity.class);
            this.auctionHouseDao = DaoManager.createDao(connection, AuctionHouseItemEntity.class);
            this.transactionsDao = DaoManager.createDao(connection, TransactionEntity.class);

            TableUtils.createTableIfNotExists(connection, IslandPlayerTrustLinkEntity.class);
            TableUtils.createTableIfNotExists(connection, PlayerEntity.class);
            TableUtils.createTableIfNotExists(connection, IslandEntity.class);
            TableUtils.createTableIfNotExists(connection, TransactionEntity.class);
            TableUtils.createTableIfNotExists(connection, GambleEntity.class);
            TableUtils.createTableIfNotExists(connection, AuctionHouseItemEntity.class);
            TableUtils.createTableIfNotExists(connection, TransactionEntity.class);

            plugin.getLogger()
                    .info("Successfully connected to database.");

            this.scheduleDbCommitTask(connection);
        } catch (SQLException e) {
            plugin.getLogger()
                    .severe("Failed to connect to database: " + e.getMessage());
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

    private void applyChange(DatabaseChange change) throws SQLException {
        switch (change) {
            case DatabaseChange.PlayerCreateOrUpdate(PlayerEntity player) -> this.playersDao.createOrUpdate(player);
            case DatabaseChange.GambleRecordAdd(GambleEntity gamble) -> this.gamblesDao.create(gamble);
            case DatabaseChange.AuctionHouseItemAdd(AuctionHouseItemEntity auctionHouseItem) ->
                    this.auctionHouseDao.create(auctionHouseItem);
            case DatabaseChange.AuctionHouseItemRemove(AuctionHouseItemEntity auctionHouseItem) ->
                    this.auctionHouseDao.delete(auctionHouseItem);
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
            default -> throw new IllegalStateException("Unexpected value: " + change);
        }
    }
}
