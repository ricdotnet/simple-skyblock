package dev.ricr.skyblock.shop;

import dev.ricr.skyblock.SimpleSkyblock;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 00 01 02 03 04 05 06 07 08
// 09 10 11 12 13 14 15 16 17
// 18 19 20 21 22 23 24 25 26
// 27 28 29 30 31 32 33 34 35
// 36 37 38 39 40 41 42 43 44
// 45 46 47 48 49 50 51 52 53

public class ShopItems {
    public record PricePair(double sellPrice, double buyPrice) {
    }

    // Map<SellPrice, BuyPrice> each
    public static final Map<Material, PricePair> BLOCKS = new LinkedHashMap<>();
    public static final Map<Material, PricePair> ITEMS = new LinkedHashMap<>();

    public static void loadShop(SimpleSkyblock plugin) {
        File dataFolder = plugin.getDataFolder();
        File shopFile = new File(dataFolder, "shop.yml");
        YamlConfiguration shopConfig = YamlConfiguration.loadConfiguration(shopFile);

        ShopItems.BLOCKS.clear();
        ShopItems.ITEMS.clear();

        // Load blocks
        List<Map<?, ?>> blocks = shopConfig.getMapList("blocks");
        for (Map<?, ?> entry : blocks) {
            loadEntry(plugin, entry, BLOCKS);
        }

        // Load items
        List<Map<?, ?>> items = shopConfig.getMapList("items");
        for (Map<?, ?> entry : items) {
            loadEntry(plugin, entry, ITEMS);
        }

        plugin.getLogger()
                .info(String.format("Loaded %d blocks and %d items from shop.yml", BLOCKS.size(), ITEMS.size()));
    }

    private static void loadEntry(SimpleSkyblock plugin, Map<?, ?> shopList, Map<Material, PricePair> targetList) {
        String materialName = String.valueOf(shopList.get("material"));
        Material material = Material.matchMaterial(materialName);

        if (material == null) {
            plugin.getLogger()
                    .warning("Invalid material: " + materialName);
            return;
        }

        double buy = shopList.containsKey("buy") ? Double.parseDouble(shopList.get("buy")
                .toString()) : -1.0;
        double sell = shopList.containsKey("sell") ? Double.parseDouble(shopList.get("sell")
                .toString()) : -1.0;

        targetList.put(material, new PricePair(sell, buy));
    }
}
