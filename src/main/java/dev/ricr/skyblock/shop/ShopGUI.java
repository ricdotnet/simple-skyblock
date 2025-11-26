package dev.ricr.skyblock.shop;

import dev.ricr.skyblock.SimpleSkyblock;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class ShopGUI implements InventoryHolder {
    @Getter
    private final Inventory inventory;
    private final SimpleSkyblock plugin;

    public ShopGUI(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Shop"));

        int slot = 0;
        for (Map.Entry<Material, BlockItems.PricePair> entry : BlockItems.SHOP_ITEMS.entrySet()) {
            Material mat = entry.getKey();
            BlockItems.PricePair prices = entry.getValue();

            ItemStack item = new ItemStack(mat, 1);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.displayName(Component.text(mat.name()));
                meta.lore(List.of(
                        Component.text("Sell: " + priceOrNotAvailable(prices.sellPrice())),
                        Component.text("Buy: " + priceOrNotAvailable(prices.buyPrice()))
                ));
                item.setItemMeta(meta);
            }

            inventory.setItem(slot++, item);
        }
    }

    private String priceOrNotAvailable(double price) {
        if (price == -1) {
            return "Not available";
        } else {
            return String.format("$%s", price);
        }
    }
}