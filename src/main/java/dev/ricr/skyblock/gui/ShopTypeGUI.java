package dev.ricr.skyblock.gui;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.enums.ShopType;
import dev.ricr.skyblock.shop.ShopItems;
import dev.ricr.skyblock.utils.InventoryUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ShopTypeGUI implements InventoryHolder, ISimpleSkyblockGUI {
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

        ItemStack closeButton = new ItemStack(Material.BARRIER, 1);
        ItemMeta closeButtonMeta = closeButton.getItemMeta();
        closeButtonMeta.displayName(Component.text("Close"));
        closeButton.setItemMeta(closeButtonMeta);

        inventory.setItem(0, blocksShop);
        inventory.setItem(1, itemsShop);
        inventory.setItem(8, closeButton);

        InventoryUtils.fillEmptySlots(inventory);
    }


    @Override
    public void handleInventoryClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        Material material = clicked.getType();
        switch (material) {
            case COBBLESTONE ->
                    player.openInventory(new ItemsListGUI(this.plugin, ShopItems.BLOCKS, ShopType.Blocks).getInventory());
            case DIAMOND ->
                    player.openInventory(new ItemsListGUI(this.plugin, ShopItems.ITEMS, ShopType.Items).getInventory());
            case BARRIER -> player.closeInventory();
        }
    }
}
