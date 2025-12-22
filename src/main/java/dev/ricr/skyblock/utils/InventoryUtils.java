package dev.ricr.skyblock.utils;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryUtils {

    public static void fillEmptySlots(Inventory inventory) {
        var glassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, glassPane);
            }
        }
    }

}
