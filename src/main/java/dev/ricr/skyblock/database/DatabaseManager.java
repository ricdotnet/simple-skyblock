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

public class DatabaseManager {
    private final SimpleSkyblock plugin;

    @Getter
    private Dao<Balance, String> balancesDao;
    @Getter
    private Dao<Sale, Integer> salesDao;
    @Getter
    private Dao<Gamble, Integer> gamblesDao;
    @Getter
    private Dao<AuctionHouse, Integer> auctionHouseDao;
    @Getter
    private Dao<AuctionHouseTransaction, Integer> auctionHouseTransactionsDao;

    public DatabaseManager(SimpleSkyblock plugin) {
        this.plugin = plugin;

        File dataFolder = this.plugin.getDataFolder();
        String databaseUrl = String.format("jdbc:sqlite:%s/%s", dataFolder.getAbsolutePath(), "database.sql");

        this.plugin.getLogger()
                .info("Connecting to database in " + databaseUrl);

        try {
            ConnectionSource connection = new JdbcConnectionSource(databaseUrl);

            this.balancesDao = DaoManager.createDao(connection, Balance.class);
            this.salesDao = DaoManager.createDao(connection, Sale.class);
            this.gamblesDao = DaoManager.createDao(connection, Gamble.class);
            this.auctionHouseDao = DaoManager.createDao(connection, AuctionHouse.class);
            this.auctionHouseTransactionsDao = DaoManager.createDao(connection, AuctionHouseTransaction.class);

            TableUtils.createTableIfNotExists(connection, Balance.class);
            TableUtils.createTableIfNotExists(connection, Sale.class);
            TableUtils.createTableIfNotExists(connection, Gamble.class);
            TableUtils.createTableIfNotExists(connection, AuctionHouse.class);
            TableUtils.createTableIfNotExists(connection, AuctionHouseTransaction.class);

            this.plugin.getLogger()
                    .info("Successfully connected to database.");
        } catch (SQLException e) {
            this.plugin.getLogger()
                    .severe("Failed to connect to database: " + e.getMessage());
        }
    }
}
