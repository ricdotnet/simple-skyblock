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

public class ShopTypeGUI implements InventoryHolder {
    @Getter
    private final Inventory inventory;
    private final SimpleSkyblock plugin;

    public ShopTypeGUI(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 9, Component.text("Select shop"));

        ItemStack blocksShop = new ItemStack(Material.COBBLESTONE, 1);
        ItemMeta blocksShopMeta = blocksShop.getItemMeta();
        blocksShopMeta.displayName(Component.text("Blocks Shop"));
        blocksShopMeta.setEnchantmentGlintOverride(true);
        blocksShop.setItemMeta(blocksShopMeta);

        ItemStack itemsShop = new ItemStack(Material.DIAMOND, 1);
        ItemMeta itemsShopMeta = itemsShop.getItemMeta();
        itemsShopMeta.displayName(Component.text("Items Shop"));
        itemsShopMeta.setEnchantmentGlintOverride(true);
        itemsShop.setItemMeta(itemsShopMeta);

        inventory.setItem(0, blocksShop);
        inventory.setItem(1, itemsShop);
    }
}
