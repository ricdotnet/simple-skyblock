package dev.ricr.skyblock.gui;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.shop.AuctionHouseItems;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AuctionHouseGUI implements InventoryHolder, ISimpleSkyblockGUI {
    private final SimpleSkyblock plugin;
    @Getter
    public Inventory inventory;

    public AuctionHouseGUI(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Auction house"));

        this.fillInventory();
    }

    @Override
    public void handleInventoryClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        // TODO: do the buying thing
    }

    private void fillInventory() {
        // TODO: update pagination logic
        List<ItemStack> items = this.plugin.auctionHouseItems.getPageOfItems(1);

        int slot = 0;
        for (ItemStack item : items) {
            inventory.setItem(slot, item);
            slot++;
        }
    }
}
