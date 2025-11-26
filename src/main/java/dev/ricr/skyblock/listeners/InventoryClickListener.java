package dev.ricr.skyblock.listeners;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.Balance;
import dev.ricr.skyblock.database.Sale;
import dev.ricr.skyblock.enums.ShopType;
import dev.ricr.skyblock.enums.TransactionType;
import dev.ricr.skyblock.shop.ShopItems;
import dev.ricr.skyblock.shop.ConfirmGUI;
import dev.ricr.skyblock.shop.ItemsListGUI;
import dev.ricr.skyblock.shop.ShopTypeGUI;
import dev.ricr.skyblock.utils.ServerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
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
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory inventory = event.getInventory();
        if (inventory.getHolder(false) instanceof ShopTypeGUI) {
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
            }
        }

        if (inventory.getHolder(false) instanceof ItemsListGUI itemsListGUI) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) {
                return;
            }

            if ("Go back".equals(ServerUtils.getTextFromComponent(clicked.getItemMeta().displayName()))) {
                player.openInventory(new ShopTypeGUI(this.plugin).getInventory());
                return;
            }

            Material material = clicked.getType();
            ShopItems.PricePair prices = null;

            switch (itemsListGUI.getShopType()) {
                case ShopType.Blocks -> prices = ShopItems.BLOCKS.get(material);
                case ShopType.Items -> prices = ShopItems.ITEMS.get(material);
            }

            if (prices == null) {
                return;
            }

            ConfirmGUI confirmGUI = new ConfirmGUI(this.plugin, material, prices, itemsListGUI.getShopType());
            player.openInventory(confirmGUI.getInventory());
        }

        if (inventory.getHolder(false) instanceof ConfirmGUI confirmGUI) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || !isGlassPaneOrBarrierBlock(clicked.getType())) {
                return;
            }

            if ("Go back".equals(ServerUtils.getTextFromComponent(clicked.getItemMeta().displayName()))) {
                switch (confirmGUI.getShopType()) {
                    case ShopType.Blocks ->
                            player.openInventory(new ItemsListGUI(this.plugin, ShopItems.BLOCKS, ShopType.Blocks).getInventory());
                    case ShopType.Items ->
                            player.openInventory(new ItemsListGUI(this.plugin, ShopItems.ITEMS, ShopType.Items).getInventory());
                }
                return;
            }

            // get item being bought or sold
            ItemStack actionableItem = inventory.getItem(13);
            if (actionableItem == null) return;

            Material material = actionableItem.getType();
            ShopItems.PricePair prices = null;

            switch (confirmGUI.getShopType()) {
                case ShopType.Blocks -> prices = ShopItems.BLOCKS.get(material);
                case ShopType.Items -> prices = ShopItems.ITEMS.get(material);
            }

            if (prices == null) {
                return;
            }

            ItemMeta meta = clicked.getItemMeta();
            String name = meta.itemName().toString();
            int itemAmount = clicked.getAmount();

            Dao<Balance, String> balanceDao = this.plugin.databaseManager.getBalanceDao();
            Dao<Sale, Integer> saleDao = this.plugin.databaseManager.getSaleDao();
            Sale sale = new Sale();

            double totalPrice;
            double finalBalance = 0;

            TransactionType transactionType;
            if (name.contains("Buy")) {
                transactionType = TransactionType.Buy;
            } else if (name.contains("Sell")) {
                transactionType = TransactionType.Sell;
            } else {
                player.sendMessage(Component.text("Something went wrong.", NamedTextColor.RED));
                return;
            }

            if (transactionType == TransactionType.Buy) {
                totalPrice = prices.buyPrice() * itemAmount;

                try {
                    Balance userBalance = balanceDao.queryForId(player.getUniqueId().toString());

                    if (userBalance.getValue() >= totalPrice) {
                        finalBalance = userBalance.getValue() - totalPrice;
                        userBalance.setValue(finalBalance);
                        balanceDao.update(userBalance);
                        sale.setUser(userBalance);

                        player.getInventory().addItem(new ItemStack(material, itemAmount));

                        player.sendMessage(Component.text(String.format("You bought %s %s for $%s", itemAmount, ServerUtils.getTextFromComponent(actionableItem.displayName()), totalPrice), NamedTextColor.GREEN));
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                    } else {
                        player.sendMessage(Component.text("You don't have enough money to buy this item.", NamedTextColor.RED));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }
                } catch (SQLException e) {
                    // ignore for now
                }
            } else {
                totalPrice = prices.sellPrice() * itemAmount;

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
                    sale.setUser(userBalance);

                    player.getInventory().removeItem(new ItemStack(material, itemAmount));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);

                    player.sendMessage(Component.text(String.format("You sold %s %s for $%s", itemAmount, ServerUtils.getTextFromComponent(actionableItem.displayName()), totalPrice), NamedTextColor.GREEN));
                } catch (SQLException e) {
                    // ignore for now
                }
            }

            try {
                sale.setItem(material.name());
                sale.setValue(totalPrice);
                sale.setQuantity(itemAmount);
                sale.setType(transactionType.toString());

                saleDao.create(sale);

                plugin.getLogger().info(String.format("Created sale entry costs %s for %s", totalPrice, player.getName()));
            } catch (SQLException e) {
                // ignore for now
            }

            player.sendMessage(Component.text(String.format("Your balance is now $%s", finalBalance), NamedTextColor.GOLD));
        }
    }

    private boolean isGlassPaneOrBarrierBlock(Material material) {
        return material == Material.GREEN_STAINED_GLASS_PANE || material == Material.RED_STAINED_GLASS_PANE || material == Material.BARRIER;
    }
}
