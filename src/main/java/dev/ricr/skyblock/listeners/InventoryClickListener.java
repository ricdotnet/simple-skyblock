package dev.ricr.skyblock.listeners;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.Balance;
import dev.ricr.skyblock.shop.BlockItems;
import dev.ricr.skyblock.shop.ConfirmGUI;
import dev.ricr.skyblock.shop.ShopGUI;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;

public class InventoryClickListener implements Listener {
    private final SimpleSkyblock plugin;

    public InventoryClickListener(SimpleSkyblock plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);

        Inventory inventory = event.getInventory();
        if (inventory.getHolder(false) instanceof ShopGUI) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            Material material = clicked.getType();
            BlockItems.PricePair prices = BlockItems.SHOP_ITEMS.get(material);

            ConfirmGUI confirmGUI = new ConfirmGUI(this.plugin, material, prices);
            player.openInventory(confirmGUI.getInventory());
        }

        if (inventory.getHolder(false) instanceof ConfirmGUI confirmGUI) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || !isGlassPane(clicked.getType())) return;

            player.sendMessage("Clicked " + clicked.getType().name());
            player.closeInventory();
        }

//        Dao<Balance, String> balanceDao = this.plugin.databaseManager.getBalanceDao();
//
//        if (event.getClick().isLeftClick()) {
//            this.plugin.getLogger().info("Player " + player.getName() + " is buying " + material.name());
//
//            try {
//                Balance userBalance = balanceDao.queryForId(player.getUniqueId().toString());
//                if (userBalance.getValue() >= prices.buyPrice()) {
//                    userBalance.setValue(userBalance.getValue() - prices.buyPrice());
//                    balanceDao.update(userBalance);
//                    player.getInventory().addItem(new ItemStack(material, 1));
//
//                    player.sendMessage("Bought " + material.name() + " for $" + prices.buyPrice());
//                    player.sendMessage("Your balance is now $" + userBalance.getValue());
//                } else {
//                    player.sendMessage("You don't have enough money to buy this item.");
//                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
//                    return;
//                }
//            } catch (SQLException e) {
//                // ignore for now
//            }
//        } else if (event.getClick().isRightClick()) {
//            this.plugin.getLogger().info("Player " + player.getName() + " is selling " + material.name());
//
//            if (!player.getInventory().contains(material)) {
//                player.sendMessage("You don't have that item in your inventory.");
//                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
//                return;
//            }
//
//            try {
//                Balance userBalance = balanceDao.queryForId(player.getUniqueId().toString());
//                userBalance.setValue(userBalance.getValue() + prices.sellPrice());
//                balanceDao.update(userBalance);
//
//                player.getInventory().removeItem(new ItemStack(material, 1));
//                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
//
//                player.sendMessage("Sold " + material.name() + " for $" + prices.sellPrice());
//                player.sendMessage("Your balance is now $" + userBalance.getValue());
//            } catch (SQLException e) {
//                // ignore for now
//            }
//        }
    }

    private boolean isGlassPane(Material material) {
        return material == Material.GREEN_STAINED_GLASS_PANE || material == Material.RED_STAINED_GLASS_PANE;
    }
}
