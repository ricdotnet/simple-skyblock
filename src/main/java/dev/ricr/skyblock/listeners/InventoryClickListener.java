package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.gui.AuctionHouseGUI;
import dev.ricr.skyblock.gui.GambleSessionGUI;
import dev.ricr.skyblock.gui.IslandGUI;
import dev.ricr.skyblock.gui.LeaderBoardGUI;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.gui.ConfirmGUI;
import dev.ricr.skyblock.gui.ItemsListGUI;
import dev.ricr.skyblock.gui.ShopTypeGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class InventoryClickListener implements Listener {
    private final SimpleSkyblock plugin;

    public InventoryClickListener(SimpleSkyblock plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        InventoryHolder inventoryHolder = event.getInventory()
                .getHolder(false);

        switch (inventoryHolder) {
            case null -> this.plugin.getLogger()
                    .warning("Inventory holder is null");
            case ShopTypeGUI shopTypeGUI -> shopTypeGUI.handleInventoryClick(event, player);
            case ItemsListGUI itemsListGUI -> itemsListGUI.handleInventoryClick(event, player);
            case ConfirmGUI confirmGUI -> confirmGUI.handleInventoryClick(event, player);
            case LeaderBoardGUI leaderBoardGUI -> leaderBoardGUI.handleInventoryClick(event);
            case GambleSessionGUI ignored -> event.setCancelled(true);
            case AuctionHouseGUI auctionHouseGUI -> auctionHouseGUI.handleInventoryClick(event, player);
            case IslandGUI islandGUI -> islandGUI.handleInventoryClick(event, player);
            default -> {/* */}
        }
    }
}
