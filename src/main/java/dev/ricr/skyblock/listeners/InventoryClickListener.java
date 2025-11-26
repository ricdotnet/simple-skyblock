package dev.ricr.skyblock.listeners;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.Balance;
import dev.ricr.skyblock.shop.BlockItems;
import dev.ricr.skyblock.shop.ConfirmGUI;
import dev.ricr.skyblock.shop.ShopGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

            Material material = clicked.getType();
            BlockItems.PricePair prices = BlockItems.SHOP_ITEMS.get(material);
            Dao<Balance, String> balanceDao = this.plugin.databaseManager.getBalanceDao();
            double finalBalance = 0;

            ItemMeta meta = clicked.getItemMeta();
            String name = meta.itemName().toString();
            int itemAmount = clicked.getAmount();
            double totalPrice = prices.buyPrice() * itemAmount;

            if (name.contains("Buy")) {
                try {
                    Balance userBalance = balanceDao.queryForId(player.getUniqueId().toString());

                    if (userBalance.getValue() >= totalPrice) {
                        finalBalance = userBalance.getValue() - totalPrice;
                        userBalance.setValue(finalBalance - totalPrice);
                        balanceDao.update(userBalance);
                        player.getInventory().addItem(new ItemStack(material, itemAmount));

                        player.sendMessage(Component.text(String.format("You bought %s %s for $%s", itemAmount, meta.displayName(), totalPrice), NamedTextColor.GREEN));
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                    } else {
                        player.sendMessage(Component.text("You don't have enough money to buy this item.", NamedTextColor.RED));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }
                } catch (SQLException e) {
                    // ignore for now
                }
            } else if (name.contains("Sell")) {
                if (!player.getInventory().contains(material, itemAmount)) {
                    player.sendMessage(Component.text("You don't have enough of that item in your inventory.", NamedTextColor.RED));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                try {
                    Balance userBalance = balanceDao.queryForId(player.getUniqueId().toString());

                    finalBalance = userBalance.getValue() + totalPrice;
                    userBalance.setValue(finalBalance);
                    balanceDao.update(userBalance);

                    player.getInventory().removeItem(new ItemStack(material, itemAmount));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);

                    player.sendMessage(Component.text(String.format("You sold %s %s for $%s", itemAmount, meta.displayName(), totalPrice), NamedTextColor.GREEN));
                } catch (SQLException e) {
                    // ignore for now
                }
            }

            player.sendMessage(Component.text(String.format("Your balance is now $%s", finalBalance), NamedTextColor.GOLD));
        }
    }

    private boolean isGlassPane(Material material) {
        return material == Material.GREEN_STAINED_GLASS_PANE || material == Material.RED_STAINED_GLASS_PANE;
    }
}
