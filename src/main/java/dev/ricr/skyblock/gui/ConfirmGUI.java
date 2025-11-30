package dev.ricr.skyblock.gui;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.AuctionHouse;
import dev.ricr.skyblock.database.AuctionHouseTransaction;
import dev.ricr.skyblock.database.Balance;
import dev.ricr.skyblock.database.Sale;
import dev.ricr.skyblock.enums.ShopType;
import dev.ricr.skyblock.enums.TransactionType;
import dev.ricr.skyblock.shop.ShopItems;
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
import java.util.UUID;

public class ConfirmGUI implements InventoryHolder, ISimpleSkyblockGUI {
    private final SimpleSkyblock plugin;
    @Getter
    private final Inventory inventory;
    @Getter
    private final ShopType shopType;
    private AuctionHouse auctionHouseItem;

    public ConfirmGUI(SimpleSkyblock plugin, Material item, ShopItems.PricePair pricePair, ShopType shopType) {
        this.plugin = plugin;
        this.shopType = shopType;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Confirm order"));

        ItemStack itemStack = new ItemStack(item, 1);
        inventory.setItem(13, itemStack);

        if (shopType == ShopType.AuctionHouse) {
            ItemStack confirmBuy = setOptionMeta(item, new ItemStack(Material.GREEN_STAINED_GLASS_PANE, 1),
                    pricePair.buyPrice(), TransactionType.Buy);
            ItemStack cancel = setOptionMeta(item, new ItemStack(Material.RED_STAINED_GLASS_PANE, 1),
                    pricePair.sellPrice(), TransactionType.Sell);

            inventory.setItem(9, confirmBuy);
            inventory.setItem(17, cancel);
        } else {
            if (pricePair.buyPrice() >= 0) {
                ItemStack buySingle = setOptionMeta(item, new ItemStack(Material.GREEN_STAINED_GLASS_PANE, 1),
                        pricePair.buyPrice(), TransactionType.Buy);
                ItemStack buyHalfStack = setOptionMeta(item, new ItemStack(Material.GREEN_STAINED_GLASS_PANE,
                        itemStack.getMaxStackSize() / 2), pricePair.buyPrice(), TransactionType.Buy);
                ItemStack buyFullStack = setOptionMeta(item, new ItemStack(Material.GREEN_STAINED_GLASS_PANE,
                        item.getMaxStackSize()), pricePair.buyPrice(), TransactionType.Buy);

                inventory.setItem(11, buySingle);
                inventory.setItem(10, buyHalfStack);
                inventory.setItem(9, buyFullStack);
            }

            if (pricePair.sellPrice() >= 0) {
                ItemStack sellSingle = setOptionMeta(item, new ItemStack(Material.RED_STAINED_GLASS_PANE, 1),
                        pricePair.sellPrice(), TransactionType.Sell);
                ItemStack sellHalfStack = setOptionMeta(item, new ItemStack(Material.RED_STAINED_GLASS_PANE,
                        itemStack.getMaxStackSize() / 2), pricePair.sellPrice(), TransactionType.Sell);
                ItemStack sellFullStack = setOptionMeta(item, new ItemStack(Material.RED_STAINED_GLASS_PANE,
                        item.getMaxStackSize()), pricePair.sellPrice(), TransactionType.Sell);

                inventory.setItem(15, sellSingle);
                inventory.setItem(16, sellHalfStack);
                inventory.setItem(17, sellFullStack);
            }

            ItemStack goBackButton = new ItemStack(Material.BARRIER, 1);
            ItemMeta meta = goBackButton.getItemMeta();
            meta.displayName(Component.text("Go back"));
            goBackButton.setItemMeta(meta);
            inventory.setItem(22, goBackButton);
        }
    }

    public ConfirmGUI(SimpleSkyblock plugin, ItemStack item, ShopType shopType) {
        this.plugin = plugin;
        this.shopType = shopType;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Confirm order"));

        inventory.setItem(13, item);

        ItemMeta itemMeta = item.getItemMeta();
        Integer itemId = itemMeta
                .getPersistentDataContainer()
                .get(SimpleSkyblock.AUCTION_HOUSE_ITEM_ID, PersistentDataType.INTEGER);

        if (itemId == null) {
            plugin.getLogger()
                    .warning("Item is not an auction house item");

            return;
        }

        try {
            auctionHouseItem = this.plugin.databaseManager.getAuctionHouseDao()
                    .queryForId(itemId);
        } catch (SQLException e) {
            // ignore for now
        }

        ItemStack confirmBuy = new ItemStack(Material.GREEN_STAINED_GLASS_PANE, 1);
        ItemMeta confirmBuyItemMeta = confirmBuy.getItemMeta();

        if (confirmBuyItemMeta != null) {
            confirmBuyItemMeta.displayName(Component.text("Confirm AH purchase"));
            confirmBuyItemMeta.lore(List.of(
                    Component.empty(),
                    Component.text()
                            .content("Price: ")
                            .append(Component.text(String.format("%s%s",
                                            ServerUtils.COIN_SYMBOL,
                                            ServerUtils.formatMoneyValue(auctionHouseItem.getPrice())),
                                    NamedTextColor.GOLD))
                            .build()
            ));
            confirmBuy.setItemMeta(confirmBuyItemMeta);
        }

        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE, 1);
        ItemMeta cancelItemMeta = cancel.getItemMeta();

        if (cancelItemMeta != null) {
            cancelItemMeta.displayName(Component.text("Cancel AH purchase"));
            cancel.setItemMeta(cancelItemMeta);
        }

        inventory.setItem(17, confirmBuy);
        inventory.setItem(9, cancel);
    }

    @Override
    public void handleInventoryClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !isGlassPaneOrBarrierBlock(clicked.getType())) {
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            switch (this.getShopType()) {
                case ShopType.Blocks ->
                        player.openInventory(new ItemsListGUI(this.plugin, ShopItems.BLOCKS, ShopType.Blocks).getInventory());
                case ShopType.Items ->
                        player.openInventory(new ItemsListGUI(this.plugin, ShopItems.ITEMS, ShopType.Items).getInventory());
                case ShopType.AuctionHouse -> player.openInventory(new AuctionHouseGUI(this.plugin).getInventory());
            }
            return;
        }

        if (player.getInventory()
                .firstEmpty() == -1) {
            player.sendMessage(Component.text("Your inventory is full", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        String clickedItemName = ServerUtils.getTextFromComponent(meta.displayName());

        // get item being bought or sold
        ItemStack actionableItem = inventory.getItem(13);
        if (actionableItem == null) {
            return;
        }

        if (clickedItemName.contains("AH")) {
            if (clicked.getType() == Material.RED_STAINED_GLASS_PANE) {
                player.openInventory(new AuctionHouseGUI(this.plugin).getInventory());
                return;
            }

            if (clicked.getType() == Material.GREEN_STAINED_GLASS_PANE) {
                if (auctionHouseItem.getOwnerName()
                        .equals(player.getName())) {
                    player.sendMessage(Component.text("You can't buy your own item.", NamedTextColor.RED));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                double price = auctionHouseItem.getPrice();

                try {
                    Balance userBalance = this.plugin.databaseManager.getBalancesDao()
                            .queryForId(player.getUniqueId()
                                    .toString());

                    if (userBalance.getValue() < price) {
                        player.sendMessage(Component.text("You don't have enough money to buy this item.",
                                NamedTextColor.RED));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }

                    userBalance.setValue(userBalance.getValue() - price);
                    this.plugin.databaseManager.getBalancesDao()
                            .update(userBalance);

                    ItemStack itemToGive = actionableItem.clone();
                    ItemMeta originalMeta = this.plugin.auctionHouseItems.getItemOriginalMeta()
                            .put(auctionHouseItem.getId(), itemToGive.getItemMeta());

                    itemToGive.setItemMeta(originalMeta);

                    player.getInventory()
                            .addItem(itemToGive);

                    this.sendMessageToSeller(auctionHouseItem.getUser()
                            .getUserId());

                    AuctionHouseTransaction transaction = new AuctionHouseTransaction();
                    transaction.setUser(userBalance);
                    transaction.setSeller(auctionHouseItem.getUser());
                    transaction.setItem(ServerUtils.base64FromBytes(itemToGive.serializeAsBytes()));
                    transaction.setPrice(price);
                    this.plugin.databaseManager.getAuctionHouseTransactionsDao()
                            .create(transaction);

                    this.plugin.databaseManager.getAuctionHouseDao()
                            .delete(auctionHouseItem);

                    Balance sellerBalance = this.plugin.databaseManager.getBalancesDao()
                            .queryForId(auctionHouseItem.getUser()
                                    .getUserId());
                    sellerBalance.setValue(sellerBalance.getValue() + price);
                    this.plugin.databaseManager.getBalancesDao()
                            .update(sellerBalance);
                } catch (SQLException e) {
                    // ignore for now
                }
            }

            player.closeInventory();
            return;
        }

        Material material = actionableItem.getType();
        ShopItems.PricePair prices = null;

        switch (this.getShopType()) {
            case ShopType.Blocks -> prices = ShopItems.BLOCKS.get(material);
            case ShopType.Items -> prices = ShopItems.ITEMS.get(material);
        }

        if (prices == null) {
            return;
        }

        int itemAmount = clicked.getAmount();

        Dao<Balance, String> balanceDao = this.plugin.databaseManager.getBalancesDao();
        Dao<Sale, Integer> saleDao = this.plugin.databaseManager.getSalesDao();
        Sale sale = new Sale();

        double totalPrice;
        double finalBalance = 0;

        TransactionType transactionType;
        if (clickedItemName.contains("Buy")) {
            transactionType = TransactionType.Buy;
        } else if (clickedItemName.contains("Sell")) {
            transactionType = TransactionType.Sell;
        } else {
            player.sendMessage(Component.text("Something went wrong.", NamedTextColor.RED));
            return;
        }

        if (transactionType == TransactionType.Buy) {
            totalPrice = prices.buyPrice() * itemAmount;

            try {
                Balance userBalance = balanceDao.queryForId(player.getUniqueId()
                        .toString());

                if (userBalance.getValue() >= totalPrice) {
                    finalBalance = userBalance.getValue() - totalPrice;
                    userBalance.setValue(finalBalance);
                    balanceDao.update(userBalance);
                    sale.setUser(userBalance);

                    player.getInventory()
                            .addItem(new ItemStack(material, itemAmount));

                    player.sendMessage(Component.text(String.format("You bought %s %s for %s%s", itemAmount,
                            ServerUtils.getTextFromComponent(actionableItem.displayName()), ServerUtils.COIN_SYMBOL,
                            ServerUtils.formatMoneyValue(totalPrice)), NamedTextColor.GREEN));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                } else {
                    player.sendMessage(Component.text("You don't have enough money to buy this item.",
                            NamedTextColor.RED));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }
            } catch (SQLException e) {
                // ignore for now
            }
        } else {
            totalPrice = prices.sellPrice() * itemAmount;

            if (!player.getInventory()
                    .contains(material, itemAmount)) {
                player.sendMessage(Component.text("You don't have enough of that item in your inventory.",
                        NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            try {
                Balance userBalance = balanceDao.queryForId(player.getUniqueId()
                        .toString());

                finalBalance = userBalance.getValue() + totalPrice;
                userBalance.setValue(finalBalance);
                balanceDao.update(userBalance);
                sale.setUser(userBalance);

                player.getInventory()
                        .removeItem(new ItemStack(material, itemAmount));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);

                player.sendMessage(Component.text(String.format("You sold %s %s for %s%s", itemAmount,
                        ServerUtils.getTextFromComponent(actionableItem.displayName()), ServerUtils.COIN_SYMBOL,
                        ServerUtils.formatMoneyValue(totalPrice)), NamedTextColor.GREEN));
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

            plugin.getLogger()
                    .info(String.format("Created sale entry costs %s%s for %s", ServerUtils.COIN_SYMBOL, totalPrice,
                            player.getName()));
        } catch (SQLException e) {
            // ignore for now
        }

        player.sendMessage(Component.text(String.format("Your balance is now %s%s", ServerUtils.COIN_SYMBOL,
                ServerUtils.formatMoneyValue(finalBalance)), NamedTextColor.GOLD));
    }

    private ItemStack setOptionMeta(Material material, ItemStack itemStack, double price,
                                    TransactionType transactionType) {
        ItemMeta meta = itemStack.getItemMeta();
        int stackSize = itemStack.getAmount();

        if (meta != null) {
            meta.displayName(Component.text(transactionType + " " + material.name()));
            meta.lore(List.of(Component.text(String.format("Total: %s%s", ServerUtils.COIN_SYMBOL, price * stackSize))));
            meta.itemName(Component.text(transactionType.name()));
            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }

    private void sendMessageToSeller(String sellerId) {
        Player seller = this.plugin.getServer()
                .getPlayer(UUID.fromString(sellerId));

        if (seller != null) {
            seller.sendMessage(Component.text("One of your auction house items sold.", NamedTextColor.GOLD));
            seller.playSound(seller.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        }
    }
}
