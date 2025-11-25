package dev.ricr.skyblock.database;

import dev.ricr.skyblock.SimpleSkyblock;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.sql.Statement;

public class Economy {
    static void createEconomyTable(SimpleSkyblock plugin) {
        String sql = """
                CREATE TABLE IF NOT EXISTS economy (
                    user_id TEXT NOT NULL PRIMARY KEY,
                    balance REAL NOT NULL DEFAULT 0
                );
                """;

        try (Statement statement = plugin.connectionManager.getConnection().createStatement()) {
            plugin.getLogger().info("Setting up Economy table");
            statement.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create Economy table: " + e.getMessage());
        }
    }
}
