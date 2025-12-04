package dev.ricr.skyblock.gui;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.AuctionHouse;
import dev.ricr.skyblock.enums.ShopType;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;
import java.util.List;

public class AuctionHouseGUI implements InventoryHolder, ISimpleSkyblockGUI {
    private final SimpleSkyblock plugin;
    private int currentPage = 1;

    @Getter
    public final Inventory inventory;

    public AuctionHouseGUI(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Auction house"));

        this.loadAuctionHouseItems();
        this.addPageAndRefreshButtons();
    }

    @Override
    public void handleInventoryClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.ARROW) {
            String guiButtonType = clicked.getItemMeta()
                    .getPersistentDataContainer()
                    .get(ServerUtils.GUI_BUTTON_TYPE, PersistentDataType.STRING);

            if (ServerUtils.AUCTION_NEXT_PAGE.equals(guiButtonType)) {
                this.currentPage++;
            } else if (ServerUtils.AUCTION_PREVIOUS_PAGE.equals(guiButtonType)) {
                this.currentPage--;
            }

            this.refreshInventory();
            return;
        }

        if (event.isRightClick()) {
            this.removeFromAuctionHouse(player, clicked);
            return;
        }

        if (ServerUtils.AUCTION_REFRESH_BUTTON.equals(clicked.getPersistentDataContainer()
                .get(ServerUtils.GUI_BUTTON_TYPE, PersistentDataType.STRING))) {
            this.refreshInventory();
            return;
        }

        ConfirmGUI confirmGUI = new ConfirmGUI(this.plugin, clicked, ShopType.AuctionHouse);
        player.openInventory(confirmGUI.getInventory());
    }

    private void loadAuctionHouseItems() {
        List<ItemStack> items = this.plugin.auctionHouseItems.getPageOfItems(this.currentPage);

        int slot = 0;
        for (ItemStack item : items) {
            inventory.setItem(slot, item);
            slot++;
        }
    }

    private void addPageAndRefreshButtons() {
        ItemStack previousPage = new ItemStack(Material.ARROW, 1);
        ItemStack nextPage = new ItemStack(Material.ARROW, 1);
        ItemStack refreshButton = new ItemStack(Material.ANVIL);
        long totalPages = this.plugin.auctionHouseItems.getTotalPages();

        ItemMeta nextPageMeta = nextPage.getItemMeta();
        nextPageMeta.displayName(Component.text("Next page", NamedTextColor.GREEN));
        nextPageMeta.getPersistentDataContainer()
                .set(ServerUtils.GUI_BUTTON_TYPE, PersistentDataType.STRING, ServerUtils.AUCTION_NEXT_PAGE);
        nextPage.setItemMeta(nextPageMeta);

        ItemMeta previousPageMeta = previousPage.getItemMeta();
        previousPageMeta.displayName(Component.text("Previous page", NamedTextColor.GREEN));
        previousPageMeta.getPersistentDataContainer()
                .set(ServerUtils.GUI_BUTTON_TYPE, PersistentDataType.STRING, ServerUtils.AUCTION_PREVIOUS_PAGE);
        previousPage.setItemMeta(previousPageMeta);

        ItemMeta refreshMeta = refreshButton.getItemMeta();
        refreshMeta.displayName(Component.text("Auction House", NamedTextColor.GREEN));
        refreshMeta.lore(List.of(Component.text(String.format("Page %s/%s - Click to refresh", currentPage,
                totalPages), NamedTextColor.WHITE)));
        refreshMeta.getPersistentDataContainer()
                .set(ServerUtils.GUI_BUTTON_TYPE, PersistentDataType.STRING, ServerUtils.AUCTION_REFRESH_BUTTON);
        refreshButton.setItemMeta(refreshMeta);

        if (currentPage == 1) {
            previousPage.setAmount(0);
        }
        if (currentPage == totalPages) {
            nextPage.setAmount(0);
        }

        inventory.setItem(45, previousPage);
        inventory.setItem(49, refreshButton);
        inventory.setItem(53, nextPage);
    }

    private void refreshInventory() {
        this.inventory.clear();
        this.loadAuctionHouseItems();
        this.addPageAndRefreshButtons();
    }

    private void removeFromAuctionHouse(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        Integer itemId = meta.getPersistentDataContainer()
                .get(ServerUtils.AUCTION_HOUSE_ITEM_ID, PersistentDataType.INTEGER);

        if (player.getInventory()
                .firstEmpty() == -1) {
            player.sendMessage(Component.text("Your inventory is full", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        try {
            AuctionHouse auctionHouse = this.plugin.databaseManager.getAuctionHouseDao()
                    .queryForId(itemId);

            if (auctionHouse == null) {
                return;
            }

            if (!auctionHouse.getUser()
                    .getUserId()
                    .equals(player.getUniqueId()
                            .toString())) {
                return;
            }

            ItemStack itemToGive = item.clone();
            ItemMeta originalMeta = this.plugin.auctionHouseItems.getItemOriginalMeta()
                    .get(itemId);
            itemToGive.setItemMeta(originalMeta);

            this.plugin.databaseManager.getAuctionHouseDao()
                    .delete(auctionHouse);

            player.getInventory()
                    .addItem(itemToGive);

            this.refreshInventory();

            player.sendMessage(Component.text(String.format("You removed %s from the auction house",
                    ServerUtils.getTextFromComponent(itemToGive.displayName())), NamedTextColor.GREEN));
        } catch (SQLException e) {
            // ignore for now
        }
    }
}
