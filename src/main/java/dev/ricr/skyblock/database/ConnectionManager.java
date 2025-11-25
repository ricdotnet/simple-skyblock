package dev.ricr.skyblock.database;

import dev.ricr.skyblock.SimpleSkyblock;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConnectionManager {
    private Connection connection;
    private final SimpleSkyblock plugin;

    public ConnectionManager(SimpleSkyblock plugin) {
        this.plugin = plugin;

        File dataFolder = this.plugin.getDataFolder();
        String databaseUrl = String.format("jdbc:sqlite:%s/%s", dataFolder.getAbsolutePath(), "database.sql");

        this.plugin.getLogger().info("Connecting to database in " + databaseUrl);

        try {
            this.connection = DriverManager.getConnection(databaseUrl);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL;");
                stmt.execute("PRAGMA foreign_keys=ON;");
            }

            this.plugin.getLogger().info("Successfully connected to database.");
        } catch (SQLException e) {
            this.plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
        } finally {
            Economy.createEconomyTable(plugin);
            this.createSalesTable();
        }
    }

    public Connection getConnection() {
        return this.connection;
    }

    private void createSalesTable() {
        // type can be either BOUGHT or SOLD
        String sql = """
                CREATE TABLE IF NOT EXISTS sales (
                    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    user_id TEXT NOT NULL,
                    value REAL NOT NULL,
                    item TEXT NOT NULL,
                    type TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES economy(user_id)
                );
                """;

        try (Statement statement = this.connection.createStatement()) {
            this.plugin.getLogger().info("Setting up Sales table");
            statement.execute(sql);
        } catch (SQLException e) {
            this.plugin.getLogger().severe("Failed to create Sales table: " + e.getMessage());
        }
    }
}
