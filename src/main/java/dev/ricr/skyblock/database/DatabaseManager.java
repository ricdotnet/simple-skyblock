package dev.ricr.skyblock.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import dev.ricr.skyblock.SimpleSkyblock;
import lombok.Getter;

import java.io.File;
import java.sql.SQLException;

@Getter
public class DatabaseManager {
    private Dao<User, String> usersDao;
    private Dao<Island, String> islandsDao;
    private Dao<Sale, Integer> salesDao;
    private Dao<Gamble, Integer> gamblesDao;
    private Dao<AuctionHouse, Integer> auctionHouseDao;
    private Dao<AuctionHouseTransaction, Integer> auctionHouseTransactionsDao;

    public DatabaseManager(SimpleSkyblock plugin) {
        File dataFolder = plugin.getDataFolder();
        String databaseUrl = String.format("jdbc:sqlite:%s/%s", dataFolder.getAbsolutePath(), "database.sql");

        plugin.getLogger()
                .info("Connecting to database in " + databaseUrl);

        try {
            ConnectionSource connection = new JdbcConnectionSource(databaseUrl);

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
        } catch (SQLException e) {
            plugin.getLogger()
                    .severe("Failed to connect to database: " + e.getMessage());
        }
    }
}
