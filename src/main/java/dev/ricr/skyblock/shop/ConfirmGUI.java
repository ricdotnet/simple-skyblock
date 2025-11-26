package dev.ricr.skyblock.shop;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.enums.TransactionType;
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
            ItemStack buySingle = setOptionMeta(item, new ItemStack(Material.GREEN_STAINED_GLASS_PANE, 1), pricePair.buyPrice(), TransactionType.Buy);
            ItemStack buyHalfStack = setOptionMeta(item, new ItemStack(Material.GREEN_STAINED_GLASS_PANE, itemStack.getMaxStackSize() / 2), pricePair.buyPrice(), TransactionType.Buy);
            ItemStack buyFullStack = setOptionMeta(item, new ItemStack(Material.GREEN_STAINED_GLASS_PANE, item.getMaxStackSize()), pricePair.buyPrice(), TransactionType.Buy);

            inventory.setItem(11, buySingle);
            inventory.setItem(10, buyHalfStack);
            inventory.setItem(9, buyFullStack);
        }

        if (pricePair.sellPrice() >= 0) {
            ItemStack sellSingle = setOptionMeta(item, new ItemStack(Material.RED_STAINED_GLASS_PANE, 1), pricePair.sellPrice(), TransactionType.Sell);
            ItemStack sellHalfStack = setOptionMeta(item, new ItemStack(Material.RED_STAINED_GLASS_PANE, itemStack.getMaxStackSize() / 2), pricePair.sellPrice(), TransactionType.Sell);
            ItemStack sellFullStack = setOptionMeta(item, new ItemStack(Material.RED_STAINED_GLASS_PANE, item.getMaxStackSize()), pricePair.sellPrice(), TransactionType.Sell);

            inventory.setItem(15, sellSingle);
            inventory.setItem(16, sellHalfStack);
            inventory.setItem(17, sellFullStack);
        }
    }

    private ItemStack setOptionMeta(Material material, ItemStack itemStack, double price, TransactionType transactionType) {
        ItemMeta meta = itemStack.getItemMeta();
        int stackSize = itemStack.getAmount();

        if (meta != null) {
            meta.displayName(Component.text(transactionType + " " + material.name()));
            meta.lore(List.of(Component.text("Total: $" + price * stackSize)));
            meta.itemName(Component.text(transactionType.name()));
            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }
}
