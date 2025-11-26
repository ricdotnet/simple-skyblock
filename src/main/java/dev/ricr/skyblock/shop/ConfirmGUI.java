package dev.ricr.skyblock.shop;

import dev.ricr.skyblock.SimpleSkyblock;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ConfirmGUI implements InventoryHolder {
    @Getter
    private final Inventory inventory;
    private final SimpleSkyblock plugin;

    public ConfirmGUI(SimpleSkyblock plugin, Material item, BlockItems.PricePair pricePair) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Confirm order"));

        // 9 10 11 _ 13 _ 15 16 17
        ItemStack itemStack = new ItemStack(item, 1);
        inventory.setItem(13, itemStack);

        if (pricePair.buyPrice() >= 0) {
            ItemStack buySingle = new ItemStack(Material.GREEN_STAINED_GLASS_PANE, 1);
            ItemStack buyHalfStack = new ItemStack(Material.GREEN_STAINED_GLASS_PANE, itemStack.getMaxStackSize() / 2);
            ItemStack buyFullStack = new ItemStack(Material.GREEN_STAINED_GLASS_PANE, item.getMaxStackSize());

            setOptionMeta(item, buySingle, 1, pricePair.buyPrice(), "Buy");
            setOptionMeta(item, buyHalfStack, itemStack.getMaxStackSize() / 2, pricePair.buyPrice(), "Buy");
            setOptionMeta(item, buyFullStack, itemStack.getMaxStackSize(), pricePair.buyPrice(), "Buy");

            inventory.setItem(11, buySingle);
            inventory.setItem(10, buyHalfStack);
            inventory.setItem(9, buyFullStack);
        }

        if (pricePair.sellPrice() >= 0) {
            ItemStack sellSingle = new ItemStack(Material.RED_STAINED_GLASS_PANE, 1);
            ItemStack sellHalfStack = new ItemStack(Material.RED_STAINED_GLASS_PANE, itemStack.getMaxStackSize() / 2);
            ItemStack sellFullStack = new ItemStack(Material.RED_STAINED_GLASS_PANE, item.getMaxStackSize());

            setOptionMeta(item, sellSingle, 1, pricePair.sellPrice(), "Sell");
            setOptionMeta(item, sellHalfStack, itemStack.getMaxStackSize() / 2, pricePair.sellPrice(), "Sell");
            setOptionMeta(item, sellFullStack, itemStack.getMaxStackSize(), pricePair.sellPrice(), "Sell");

            inventory.setItem(15, sellSingle);
            inventory.setItem(16, sellHalfStack);
            inventory.setItem(17, sellFullStack);
        }
    }

    private void setOptionMeta(Material material, ItemStack itemStack, int stackSize, double price, String transactionType) {
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text(transactionType + " " + material.name()));
            meta.lore(List.of(Component.text("Total: $" + price * stackSize)));
            itemStack.setItemMeta(meta);
        }
    }
}
