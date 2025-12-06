package dev.ricr.skyblock;

import dev.ricr.skyblock.commands.AuctionHouseCommand;
import dev.ricr.skyblock.commands.BalanceCommand;
import dev.ricr.skyblock.commands.GambleCommand;
import dev.ricr.skyblock.commands.IslandCommand;
import dev.ricr.skyblock.commands.LeaderboardCommand;
import dev.ricr.skyblock.commands.PayCommand;
import dev.ricr.skyblock.commands.ReloadShopCommand;
import dev.ricr.skyblock.commands.ShopCommand;
import dev.ricr.skyblock.database.DatabaseManager;
import dev.ricr.skyblock.generators.IslandGenerator;
import dev.ricr.skyblock.generators.StrongholdGenerator;
import dev.ricr.skyblock.listeners.ChatListener;
import dev.ricr.skyblock.listeners.ChunkLoadListener;
import dev.ricr.skyblock.listeners.InventoryClickListener;
import dev.ricr.skyblock.listeners.IslandListeners;
import dev.ricr.skyblock.listeners.PlayerJoinListener;
import dev.ricr.skyblock.listeners.PlayerRespawnListener;
import dev.ricr.skyblock.shop.AuctionHouseItems;
import dev.ricr.skyblock.shop.ShopItems;
import dev.ricr.skyblock.utils.IslandManager;
import dev.ricr.skyblock.utils.ServerUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public class SimpleSkyblock extends JavaPlugin {
    public FileConfiguration serverConfig;
    public DatabaseManager databaseManager;
    public IslandManager islandManager;
    public AuctionHouseItems auctionHouseItems;

    @Override
    public void onEnable() {
        File dataFolder = this.ensureDataFolderExists();

        // We load the server config into memory for fast access
        // Any changes to it, we then trigger a save
        this.serverConfig = ServerUtils.loadConfig(dataFolder);

        this.createDefaultShopConfigAndLoadShopItems();

        // Open managers
        this.databaseManager = new DatabaseManager(this);
        this.islandManager = new IslandManager(this);

        // Open an auction house class with fast access Dao
        this.auctionHouseItems = new AuctionHouseItems(this);

        StrongholdGenerator strongholdGenerator = new StrongholdGenerator(this, serverConfig);
        IslandGenerator islandGenerator = new IslandGenerator(this, serverConfig);

        // TODO: refactor a bit more
        this.getServer().getPluginManager()
                .registerEvents(new ChatListener(), this);
        this.getServer().getPluginManager()
                .registerEvents(new PlayerJoinListener(this, islandGenerator), this);
        this.getServer().getPluginManager()
                .registerEvents(new ChunkLoadListener(this, strongholdGenerator), this);
        this.getServer().getPluginManager()
                .registerEvents(new InventoryClickListener(this), this);
        this.getServer().getPluginManager()
                .registerEvents(new PlayerRespawnListener(this), this);
        this.getServer().getPluginManager()
                .registerEvents(new IslandListeners(this), this);

        // Register commands
        Objects.requireNonNull(this.getCommand("balance"))
                .setExecutor(new BalanceCommand(this));
        Objects.requireNonNull(this.getCommand("pay"))
                .setExecutor(new PayCommand(this));
        Objects.requireNonNull(this.getCommand("shop"))
                .setExecutor(new ShopCommand(this));
        Objects.requireNonNull(this.getCommand("reloadshop"))
                .setExecutor(new ReloadShopCommand(this));
        Objects.requireNonNull(this.getCommand("leaderboard"))
                .setExecutor(new LeaderboardCommand(this));
        Objects.requireNonNull(this.getCommand("gamble"))
                .setExecutor(new GambleCommand(this));
        Objects.requireNonNull(this.getCommand("auctionhouse"))
                .setExecutor(new AuctionHouseCommand(this));
        Objects.requireNonNull(this.getCommand("island"))
                .setExecutor(new IslandCommand(this));

        // Initiate static namespaced keys
        ServerUtils.initiateNamespacedKeys(this);

        this.getLogger().info("SimpleSkyblock has been enabled!");
    }

    @Override
    public void onDisable() {
        this.getLogger().info("SimpleSkyblock has been disabled!");
    }

    private File ensureDataFolderExists() {
        File dataFolder = this.getDataFolder();

        if (!dataFolder.exists()) {
            boolean dataFolderGenerated = dataFolder.mkdirs();
            if (!dataFolderGenerated) {
                this.getLogger().severe("Could not create data folder!");
                this.getServer().getPluginManager()
                        .disablePlugin(this);
            }
        }

        return this.getDataFolder();
    }

    private void createDefaultShopConfigAndLoadShopItems() {
        File file = new File(this.getDataFolder(), "shop.yml");
        if (!file.exists()) {
            this.saveResource("shop.yml", false);
        }

        ShopItems.loadShop(this);
    }
}

