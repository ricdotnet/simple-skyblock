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
    private Dao<Balance, String> balanceDao;
    @Getter
    private Dao<Sale, Integer> saleDao;

    public DatabaseManager(SimpleSkyblock plugin) {
        this.plugin = plugin;

        File dataFolder = this.plugin.getDataFolder();
        String databaseUrl = String.format("jdbc:sqlite:%s/%s", dataFolder.getAbsolutePath(), "database.sql");

        this.plugin.getLogger().info("Connecting to database in " + databaseUrl);

        try {
            ConnectionSource connection = new JdbcConnectionSource(databaseUrl);

            this.balanceDao = DaoManager.createDao(connection, Balance.class);
            this.saleDao = DaoManager.createDao(connection, Sale.class);

            TableUtils.createTableIfNotExists(connection, Balance.class);
            TableUtils.createTableIfNotExists(connection, Sale.class);

            this.plugin.getLogger().info("Successfully connected to database.");
        } catch (SQLException e) {
            this.plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
        }
    }
}
