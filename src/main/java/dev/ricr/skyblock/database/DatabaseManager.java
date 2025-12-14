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

    private Dao<IslandUserTrustLink, String> islandUserTrustLinksDao;
    private Dao<User, String> usersDao;
    private Dao<Island, String> islandsDao;
    private Dao<Sale, Integer> salesDao;
    private Dao<Gamble, Integer> gamblesDao;
    private Dao<AuctionHouse, Integer> auctionHouseDao;
    private Dao<AuctionHouseTransaction, Integer> auctionHouseTransactionsDao;

    public DatabaseManager(SimpleSkyblock plugin, DatabaseChangesAccumulator accumulator) {
        this.plugin = plugin;
        this.accumulator = accumulator;
        File dataFolder = plugin.getDataFolder();
        String databaseUrl = String.format("jdbc:sqlite:%s/%s", dataFolder.getAbsolutePath(), "database.sql");

        plugin.getLogger()
                .info("Connecting to database in " + databaseUrl);

        try {
            ConnectionSource connection = new JdbcConnectionSource(databaseUrl);

            this.islandUserTrustLinksDao = DaoManager.createDao(connection, IslandUserTrustLink.class);
            this.usersDao = DaoManager.createDao(connection, User.class);
            this.islandsDao = DaoManager.createDao(connection, Island.class);
            this.salesDao = DaoManager.createDao(connection, Sale.class);
            this.gamblesDao = DaoManager.createDao(connection, Gamble.class);
            this.auctionHouseDao = DaoManager.createDao(connection, AuctionHouse.class);
            this.auctionHouseTransactionsDao = DaoManager.createDao(connection, AuctionHouseTransaction.class);

            TableUtils.createTableIfNotExists(connection, IslandUserTrustLink.class);
            TableUtils.createTableIfNotExists(connection, User.class);
            TableUtils.createTableIfNotExists(connection, Island.class);
            TableUtils.createTableIfNotExists(connection, Sale.class);
            TableUtils.createTableIfNotExists(connection, Gamble.class);
            TableUtils.createTableIfNotExists(connection, AuctionHouse.class);
            TableUtils.createTableIfNotExists(connection, AuctionHouseTransaction.class);

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
            case DatabaseChange.UserCreateOrUpdate(User player) -> this.usersDao.createOrUpdate(player);
            case DatabaseChange.GambleRecordAdd(Gamble gamble) -> this.gamblesDao.create(gamble);
            case DatabaseChange.SaleRecordAdd(Sale sale) -> this.salesDao.create(sale);
            case DatabaseChange.AuctionHouseItemAdd(AuctionHouse auctionHouse) ->
                    this.auctionHouseDao.create(auctionHouse);
            case DatabaseChange.AuctionHouseItemRemove(AuctionHouse auctionHouse) ->
                    this.auctionHouseDao.delete(auctionHouse);
            case DatabaseChange.AuctionHouseTransactionAdd(AuctionHouseTransaction auctionHouseTransaction) ->
                    this.auctionHouseTransactionsDao.create(auctionHouseTransaction);
            case DatabaseChange.TrustedPlayerAdd(Island userIsland, User targetUser) -> {
                var islandPlayerTrustLink = new IslandUserTrustLink();

                islandPlayerTrustLink.setIsland(userIsland);
                islandPlayerTrustLink.setUser(targetUser);

                this.islandUserTrustLinksDao.create(islandPlayerTrustLink);
            }
            case DatabaseChange.TrustedPlayerRemove(String islandOwnerId, String trustedPlayerId) -> {
                var deleteBuilder = this.plugin.databaseManager.getIslandUserTrustLinksDao().deleteBuilder();
                deleteBuilder.where()
                        .eq("island_id", islandOwnerId)
                        .and()
                        .eq("user_id", trustedPlayerId);
                deleteBuilder.delete();
            }
            default -> throw new IllegalStateException("Unexpected value: " + change);
        }
    }
}
