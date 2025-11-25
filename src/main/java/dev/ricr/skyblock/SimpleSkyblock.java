package dev.ricr.skyblock;

import dev.ricr.skyblock.database.ConnectionManager;
import dev.ricr.skyblock.generators.IslandGenerator;
import dev.ricr.skyblock.generators.StrongholdGenerator;
import dev.ricr.skyblock.listeners.ChunkLoadListener;
import dev.ricr.skyblock.listeners.PlayerJoinListener;
import dev.ricr.skyblock.listeners.PlayerUseListener;
import dev.ricr.skyblock.utils.ServerUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;

public class SimpleSkyblock extends JavaPlugin {
    public ConnectionManager connectionManager;

    @Override
    public void onEnable() {
        File dataFolder = getDataFolder();

        if (!dataFolder.exists()) {
            boolean dataFolderGenerated = dataFolder.mkdirs();
            if (!dataFolderGenerated) {
                getLogger().severe("Could not create data folder!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        // Connect to a simple sqlite database
        this.connectionManager = new ConnectionManager(this);

        // We load the server config into memory for fast access
        // Any changes to it, we then trigger a save
        FileConfiguration serverConfig = ServerUtils.loadConfig(dataFolder);

        StrongholdGenerator strongholdGenerator = new StrongholdGenerator(this, serverConfig);
        IslandGenerator islandGenerator = new IslandGenerator(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(islandGenerator), this);
        getServer().getPluginManager().registerEvents(new ChunkLoadListener(strongholdGenerator), this);
        getServer().getPluginManager().registerEvents(new PlayerUseListener(this, serverConfig), this);

        getLogger().info("SimpleSkyblock has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("SimpleSkyblock has been disabled!");

        if  (connectionManager != null) {
            try {
                this.connectionManager.getConnection().close();
            } catch (SQLException e) {
                getLogger().severe("Failed to close the database connection: " + e.getMessage());
            }
        }
    }
}

