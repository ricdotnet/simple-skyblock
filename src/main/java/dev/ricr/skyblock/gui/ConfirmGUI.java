package dev.ricr.skyblock.gui;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.AuctionHouse;
import dev.ricr.skyblock.database.AuctionHouseTransaction;
import dev.ricr.skyblock.database.DatabaseChange;
import dev.ricr.skyblock.database.Sale;
import dev.ricr.skyblock.enums.ShopType;
import dev.ricr.skyblock.enums.TransactionType;
import dev.ricr.skyblock.shop.ShopItems;
import dev.ricr.skyblock.utils.PlayerUtils;
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

    public ConfirmGUI(SimpleSkyblock plugin, Player player, Material item, ShopItems.PricePair pricePair,
                      ShopType shopType) {
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

                int totalInPlayerInventory = PlayerUtils.getAllItemsInInventoryOfItem(player, item);

                ItemStack sellAll = new ItemStack(Material.TNT, 1);
                ItemMeta sellAllItemMeta = sellAll.getItemMeta();
                sellAllItemMeta.displayName(Component.text("Sell all " + item.name()));
                sellAllItemMeta.lore(List.of(Component.text(String.format("Total: %s%s", ServerUtils.COIN_SYMBOL,
                        pricePair.sellPrice() * totalInPlayerInventory))));
                sellAll.setItemMeta(sellAllItemMeta);

                inventory.setItem(15, sellSingle);
                inventory.setItem(16, sellHalfStack);
                inventory.setItem(17, sellFullStack);
                inventory.setItem(26, sellAll);
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
                .get(ServerUtils.AUCTION_HOUSE_ITEM_ID, PersistentDataType.INTEGER);

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
        if (clicked == null || clicked.getType() == Material.AIR || !isGlassPaneOrBarrierBlockOrTnt(clicked.getType())) {
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

                if (isPlayerInventoryFull(player)) {
                    player.sendMessage(Component.text("Your inventory is full", NamedTextColor.RED));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                double price = auctionHouseItem.getPrice();

                var buyerPlayerRecord = this.plugin.onlinePlayers.getPlayer(player.getUniqueId());

                if (buyerPlayerRecord.getBalance() < price) {
                    player.sendMessage(Component.text("You don't have enough money to buy this item.",
                            NamedTextColor.RED));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                buyerPlayerRecord.setBalance(buyerPlayerRecord.getBalance() - price);

                ItemStack itemToGive = actionableItem.clone();
                ItemMeta originalMeta = this.plugin.auctionHouseItems.getItemOriginalMeta()
                        .put(auctionHouseItem.getId(), itemToGive.getItemMeta());

                itemToGive.setItemMeta(originalMeta);

                player.getInventory()
                        .addItem(itemToGive);

                player.sendMessage(Component.text(String.format("Bought %s %s from %s for %s%s",
                        itemToGive.getAmount(),
                        ServerUtils.getTextFromComponent(actionableItem.displayName()),
                        auctionHouseItem.getOwnerName(), ServerUtils.COIN_SYMBOL,
                        ServerUtils.formatMoneyValue(auctionHouseItem.getPrice())), NamedTextColor.GREEN));
                this.sendMessageToSeller(auctionHouseItem.getUser()
                                .getUserId(), player,
                        ServerUtils.getTextFromComponent(actionableItem.displayName()),
                        price);

                var transaction = new AuctionHouseTransaction();
                transaction.setUser(buyerPlayerRecord);
                transaction.setSeller(auctionHouseItem.getUser());
                transaction.setItem(ServerUtils.base64FromBytes(itemToGive.serializeAsBytes()));
                transaction.setPrice(price);

                var auctionHouseTransactionAdd = new DatabaseChange.AuctionHouseTransactionAdd(transaction);
                this.plugin.databaseChangesAccumulator.add(auctionHouseTransactionAdd);

                var auctionHouseItemRemove = new DatabaseChange.AuctionHouseItemRemove(auctionHouseItem);
                this.plugin.databaseChangesAccumulator.add(auctionHouseItemRemove);

                var playerCreateOrUpdateBuyer = new DatabaseChange.UserCreateOrUpdate(buyerPlayerRecord);
                this.plugin.databaseChangesAccumulator.add(playerCreateOrUpdateBuyer);

                try {
                    var sellerPlayerRecord = this.plugin.databaseManager
                            .getUsersDao()
                            .queryForId(auctionHouseItem.getUser().getUserId());
                    sellerPlayerRecord.setBalance(sellerPlayerRecord.getBalance() + price);

                    var playerCreateOrUpdateSeller = new DatabaseChange.UserCreateOrUpdate(sellerPlayerRecord);
                    this.plugin.databaseChangesAccumulator.add(playerCreateOrUpdateSeller);
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

        // we made TNT be for selling all of 1 item
        if (clicked.getType() == Material.TNT) {
            itemAmount = PlayerUtils.getAllItemsInInventoryOfItem(player, material);
            if (itemAmount == 0) {
                player.sendMessage(Component.text("You don't have any of that item in your inventory.",
                        NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
        }

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

        var playerRecord = this.plugin.onlinePlayers.getPlayer(player.getUniqueId());
        var saleRecord = new Sale();

        if (transactionType == TransactionType.Buy) {
            if (isPlayerInventoryFull(player)) {
                player.sendMessage(Component.text("Your inventory is full", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            totalPrice = prices.buyPrice() * itemAmount;

            if (playerRecord.getBalance() >= totalPrice) {
                finalBalance = playerRecord.getBalance() - totalPrice;
                playerRecord.setBalance(finalBalance);

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
        } else {
            totalPrice = prices.sellPrice() * itemAmount;
            boolean playerHasItem = player.getInventory()
                    .contains(material, itemAmount);

            if (!playerHasItem) {
                player.sendMessage(Component.text("You don't have enough of that item in your inventory.",
                        NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            finalBalance = playerRecord.getBalance() + totalPrice;
            playerRecord.setBalance(finalBalance);

            player.getInventory()
                    .removeItem(new ItemStack(material, itemAmount));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);

            player.sendMessage(Component.text(String.format("You sold %s %s for %s%s", itemAmount,
                    ServerUtils.getTextFromComponent(actionableItem.displayName()), ServerUtils.COIN_SYMBOL,
                    ServerUtils.formatMoneyValue(totalPrice)), NamedTextColor.GREEN));
        }

        saleRecord.setItem(material.name());
        saleRecord.setValue(totalPrice);
        saleRecord.setQuantity(itemAmount);
        saleRecord.setUser(playerRecord);
        saleRecord.setType(transactionType.toString());

        var userCreateOrUpdate = new DatabaseChange.UserCreateOrUpdate(playerRecord);
        this.plugin.databaseChangesAccumulator.add(userCreateOrUpdate);

        var saleRecordAdd = new DatabaseChange.SaleRecordAdd(saleRecord);
        this.plugin.databaseChangesAccumulator.add(saleRecordAdd);

        plugin.getLogger()
                .info(String.format("Created sale entry costs %s%s for %s", ServerUtils.COIN_SYMBOL, totalPrice,
                        player.getName()));

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

    private void sendMessageToSeller(String sellerId, Player buyer, String itemName, double price) {
        Player seller = this.plugin.getServer()
                .getPlayer(UUID.fromString(sellerId));

        if (seller != null) {
            seller.sendMessage(Component.text(String.format("%s bought your %s from the action house for %s%s",
                            buyer.getName(), itemName, ServerUtils.COIN_SYMBOL, ServerUtils.formatMoneyValue(price)),
                    NamedTextColor.GOLD));
            seller.playSound(seller.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        }
    }

    private boolean isPlayerInventoryFull(Player player) {
        return player.getInventory()
                .firstEmpty() == -1;
    }
}
