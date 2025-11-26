package dev.ricr.skyblock.shop;

import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.Map;

public class BlockItems {
    public record PricePair(double sellPrice, double buyPrice) {
    }

    // Map<SellPrice, BuyPrice> each
    public static final Map<Material, PricePair> SHOP_ITEMS = new LinkedHashMap<>();

    static {
        SHOP_ITEMS.put(Material.COBBLESTONE, new PricePair(1.0, 50.0));
        SHOP_ITEMS.put(Material.STONE, new PricePair(3.0, 100.0));
        SHOP_ITEMS.put(Material.DIRT, new PricePair(-1.0, 1000.0));
        SHOP_ITEMS.put(Material.OBSIDIAN, new PricePair(-1.0, 10000.0));
    };
}
