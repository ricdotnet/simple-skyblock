package dev.ricr.skyblock;

import dev.ricr.skyblock.commands.AdminCommand;
import dev.ricr.skyblock.commands.AuctionHouseCommand;
import dev.ricr.skyblock.commands.BalanceCommand;
import dev.ricr.skyblock.commands.GambleCommand;
import dev.ricr.skyblock.commands.IslandCommand;
import dev.ricr.skyblock.commands.LeaderboardCommand;
import dev.ricr.skyblock.commands.LobbyCommand;
import dev.ricr.skyblock.commands.PayCommand;
import dev.ricr.skyblock.commands.ShopCommand;
import dev.ricr.skyblock.database.DatabaseChangesAccumulator;
import dev.ricr.skyblock.database.DatabaseManager;
import dev.ricr.skyblock.generators.IslandGenerator;
import dev.ricr.skyblock.listeners.ChatListener;
import dev.ricr.skyblock.listeners.InventoryClickListener;
import dev.ricr.skyblock.listeners.IslandListeners;
import dev.ricr.skyblock.listeners.PlayerListeners;
import dev.ricr.skyblock.listeners.ServerLoadListener;
import dev.ricr.skyblock.shop.AuctionHouseItems;
import dev.ricr.skyblock.shop.ShopItems;
import dev.ricr.skyblock.utils.IslandManager;
import dev.ricr.skyblock.utils.ServerUtils;
import dev.ricr.skyblock.utils.VoidWorldGenerator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.SQLException;
import java.util.Objects;

public class SimpleSkyblock extends JavaPlugin {
    public FileConfiguration serverConfig;
    public DatabaseManager databaseManager;
    public IslandManager islandManager;
    public AuctionHouseItems auctionHouseItems;
    public IslandGenerator islandGenerator;
    public DatabaseChangesAccumulator databaseChangesAccumulator;
    public OnlinePlayers onlinePlayers;

    @Override
    public void onEnable() {
        this.ensureDataFolderExists();
        this.createAndLoadServerConfig();
        this.createAndLoadServerShop();

        // Simple online players cache to help with batching PlayerEntity related db operations
        this.onlinePlayers = new OnlinePlayers();

        // Open managers
        this.databaseChangesAccumulator = new DatabaseChangesAccumulator(this);

        this.databaseManager = new DatabaseManager(this, this.databaseChangesAccumulator);
        this.islandManager = new IslandManager(this);

        // Open an auction house class with fast access Dao
        this.auctionHouseItems = new AuctionHouseItems(this);

        // Instantiate the island generator
        this.islandGenerator = new IslandGenerator(this);

        // Register listeners
        new ServerLoadListener(this);
        new ChatListener(this);
        new PlayerListeners(this);
        new InventoryClickListener(this);
        new IslandListeners(this);

        // Register commands
        new AdminCommand(this).register();
        new IslandCommand(this).register();
        new GambleCommand(this).register();
        new PayCommand(this).register();

        Objects.requireNonNull(this.getCommand("lobby"))
                .setExecutor(new LobbyCommand(this));
        Objects.requireNonNull(this.getCommand("balance"))
                .setExecutor(new BalanceCommand(this));
        Objects.requireNonNull(this.getCommand("shop"))
                .setExecutor(new ShopCommand(this));
        Objects.requireNonNull(this.getCommand("leaderboard"))
                .setExecutor(new LeaderboardCommand(this));

        // TODO: refactor into command tree
        Objects.requireNonNull(this.getCommand("auctionhouse"))
                .setExecutor(new AuctionHouseCommand(this));

        // Initiate static namespaced keys
        ServerUtils.initiateNamespacedKeys(this);

        this.getLogger().info("SimpleSkyblock has been enabled!");
    }

    @Override
    public void onDisable() {
        ServerUtils.cleanUpTextDisplays(this);

        try {
            this.databaseManager.commitImmediately();
        } catch (SQLException e) {
            this.getLogger().severe("Failed to commit changes to database on shutdown:");
            this.getLogger().severe(e.getMessage());
        }

        this.getLogger().info("SimpleSkyblock has been disabled!");
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, String id) {
        this.getLogger().info(String.format("Chunk generator for %s is %s", id, worldName));
        return new VoidWorldGenerator();
    }

    private void ensureDataFolderExists() {
        File dataFolder = this.getDataFolder();

        if (!dataFolder.exists()) {
            boolean dataFolderGenerated = dataFolder.mkdirs();
            if (!dataFolderGenerated) {
                this.getLogger().severe("Could not create data folder!");
                this.getServer().getPluginManager()
                        .disablePlugin(this);
            }
        }
    }

    private void createAndLoadServerConfig() {
        this.saveDefaultConfig();
        this.serverConfig = ServerUtils.loadConfig(this);
    }

    private void createAndLoadServerShop() {
        this.saveResource("shop.yml", false);
        ShopItems.loadShop(this);
    }
}

