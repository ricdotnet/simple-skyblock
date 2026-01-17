package dev.ricr.skyblock;

import dev.ricr.skyblock.commands.PluginCommands;
import dev.ricr.skyblock.database.DatabaseChangesAccumulator;
import dev.ricr.skyblock.database.DatabaseManager;
import dev.ricr.skyblock.generators.IslandGenerator;
import dev.ricr.skyblock.listeners.BarterListener;
import dev.ricr.skyblock.listeners.BaseListeners;
import dev.ricr.skyblock.listeners.ChatListener;
import dev.ricr.skyblock.listeners.FeedbackListeners;
import dev.ricr.skyblock.listeners.InventoryClickListener;
import dev.ricr.skyblock.listeners.IslandListeners;
import dev.ricr.skyblock.listeners.LuckyEnchantmentListener;
import dev.ricr.skyblock.listeners.PlayerListeners;
import dev.ricr.skyblock.listeners.ServerLoadListener;
import dev.ricr.skyblock.listeners.SilenceMobListener;
import dev.ricr.skyblock.listeners.VillagerShopInteractListener;
import dev.ricr.skyblock.shop.AuctionHouseItems;
import dev.ricr.skyblock.shop.ShopItems;
import dev.ricr.skyblock.utils.IslandManager;
import dev.ricr.skyblock.utils.ServerUtils;
import dev.ricr.skyblock.utils.VillagerShopManager;
import dev.ricr.skyblock.utils.VoidWorldGenerator;
import dev.ricr.skyblock.utils.WorldManager;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Registry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class SimpleSkyblock extends JavaPlugin {
    public FileConfiguration serverConfig;
    public DatabaseManager databaseManager;
    public IslandManager islandManager;
    public AuctionHouseItems auctionHouseItems;
    public IslandGenerator islandGenerator;
    public DatabaseChangesAccumulator databaseChangesAccumulator;
    public OnlinePlayers onlinePlayers;
    public MiniMessage miniMessage;
    public WorldManager worldManager;
    public VillagerShopManager villagerShopManager;

    public static final Registry<Enchantment> ENCHANTMENTS = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);

    @Override
    public void onEnable() {
        this.ensureDataFolderExists();
        this.createAndLoadServerConfig();
        this.createAndLoadServerShop();

        this.miniMessage = MiniMessage.miniMessage();
        this.worldManager = new WorldManager(this);

        // Simple online players cache to help with batching PlayerEntity related db operations
        this.onlinePlayers = new OnlinePlayers(this);

        // Open managers
        this.databaseChangesAccumulator = new DatabaseChangesAccumulator(this);

        this.databaseManager = new DatabaseManager(this, this.databaseChangesAccumulator);
        try {
            this.databaseManager.runMigrations(this.getFile());
        } catch (IOException e) {
            this.getLogger().severe(
                    String.format("Failed when trying to close the jar file after running migrations: %s", e.getMessage())
            );
        }

        this.islandManager = new IslandManager(this);

        // Open an auction house class with fast access Dao
        this.auctionHouseItems = new AuctionHouseItems(this);

        // Instantiate the island generator
        this.islandGenerator = new IslandGenerator(this);

        // Register listeners
        new FeedbackListeners(this);
        new BaseListeners(this);
        new PlayerListeners(this);
        new IslandListeners(this);
        new ChatListener(this);
        new ServerLoadListener(this);
        new InventoryClickListener(this);
        new BarterListener(this);
        new LuckyEnchantmentListener(this);
        new VillagerShopInteractListener(this);
        new SilenceMobListener(this);

        // Register commands
        PluginCommands.register(this);

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
        return new VoidWorldGenerator();
    }

    public void loadVillagerShops() {
        this.villagerShopManager = new VillagerShopManager(this);
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

