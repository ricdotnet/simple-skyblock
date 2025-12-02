package dev.ricr.skyblock;

import dev.ricr.skyblock.commands.AuctionHouseCommand;
import dev.ricr.skyblock.commands.BalanceCommand;
import dev.ricr.skyblock.commands.GambleCommand;
import dev.ricr.skyblock.commands.HideBorderCommand;
import dev.ricr.skyblock.commands.LeaderboardCommand;
import dev.ricr.skyblock.commands.PayCommand;
import dev.ricr.skyblock.commands.ReloadShopCommand;
import dev.ricr.skyblock.commands.ShopCommand;
import dev.ricr.skyblock.commands.ShowBorderCommand;
import dev.ricr.skyblock.database.DatabaseManager;
import dev.ricr.skyblock.generators.IslandGenerator;
import dev.ricr.skyblock.generators.StrongholdGenerator;
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
        File dataFolder = getDataFolder();

        if (!dataFolder.exists()) {
            boolean dataFolderGenerated = dataFolder.mkdirs();
            if (!dataFolderGenerated) {
                getLogger().severe("Could not create data folder!");
                getServer().getPluginManager()
                        .disablePlugin(this);
                return;
            }
        }

        // We load the server config into memory for fast access
        // Any changes to it, we then trigger a save
        serverConfig = ServerUtils.loadConfig(dataFolder);

        createDefaultShopConfigAndLoadShopItems();

        // Open managers
        this.databaseManager = new DatabaseManager(this);
        this.islandManager = new IslandManager(this);

        // Open an auction house class with fast access Dao
        this.auctionHouseItems = new AuctionHouseItems(this);

        StrongholdGenerator strongholdGenerator = new StrongholdGenerator(this, serverConfig);
        IslandGenerator islandGenerator = new IslandGenerator(this, serverConfig);

        // Register listeners
        getServer().getPluginManager()
                .registerEvents(new PlayerJoinListener(this, islandGenerator), this);
        getServer().getPluginManager()
                .registerEvents(new ChunkLoadListener(this, strongholdGenerator), this);
        getServer().getPluginManager()
                .registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager()
                .registerEvents(new PlayerRespawnListener(this), this);

        getServer().getPluginManager()
                .registerEvents(new IslandListeners(this), this);

        // Register commands
        Objects.requireNonNull(getCommand("balance"))
                .setExecutor(new BalanceCommand(this));
        Objects.requireNonNull(getCommand("pay"))
                .setExecutor(new PayCommand(this));
        Objects.requireNonNull(getCommand("shop"))
                .setExecutor(new ShopCommand(this));
        Objects.requireNonNull(getCommand("reloadshop"))
                .setExecutor(new ReloadShopCommand(this));
        Objects.requireNonNull(getCommand("leaderboard"))
                .setExecutor(new LeaderboardCommand(this));
        Objects.requireNonNull(getCommand("gamble"))
                .setExecutor(new GambleCommand(this));
        Objects.requireNonNull(getCommand("auctionhouse"))
                .setExecutor(new AuctionHouseCommand(this));
        Objects.requireNonNull(getCommand("showborder"))
                .setExecutor(new ShowBorderCommand(this));
        Objects.requireNonNull(getCommand("hideborder"))
                .setExecutor(new HideBorderCommand(this));

        // Initiate static namespaced keys
        ServerUtils.initiateNamespacedKeys(this);

        getLogger().info("SimpleSkyblock has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("SimpleSkyblock has been disabled!");
    }

    private void createDefaultShopConfigAndLoadShopItems() {
        File file = new File(getDataFolder(), "shop.yml");
        if (!file.exists()) {
            saveResource("shop.yml", false);
        }

        ShopItems.loadShop(this);
    }
}

