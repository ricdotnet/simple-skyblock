package dev.ricr.skyblock.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public interface ISimpleSkyblockGUI {

    default void handleInventoryClick(InventoryClickEvent event) {}
    default void handleInventoryClick(InventoryClickEvent event, Player player) {}

    default boolean isGlassPaneOrBarrierBlock(Material material) {
        return material == Material.GREEN_STAINED_GLASS_PANE || material == Material.RED_STAINED_GLASS_PANE || material == Material.BARRIER;
    }

}
