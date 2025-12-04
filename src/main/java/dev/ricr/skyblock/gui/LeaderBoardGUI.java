package dev.ricr.skyblock.gui;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.Balance;
import dev.ricr.skyblock.database.Sale;
import dev.ricr.skyblock.enums.TransactionType;
import dev.ricr.skyblock.utils.PlayerUtils;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class LeaderBoardGUI implements InventoryHolder, ISimpleSkyblockGUI {
    @Getter
    private final Inventory inventory;

    public LeaderBoardGUI(SimpleSkyblock plugin) {
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Balance leaderboard"));

        Dao<Balance, String> balanceDao = plugin.databaseManager.getBalancesDao();
        Dao<Sale, Integer> saleDao = plugin.databaseManager.getSalesDao();

        try {
            List<Balance> balances = balanceDao.queryBuilder()
                    .orderBy("value", false)
                    .query();
            List<Sale> sales = saleDao.queryForAll();

            double totalEconomyValue = balances.stream()
                    .mapToDouble(Balance::getValue)
                    .sum();
            double totalServerBought =
                    sales.stream()
                            .filter(sale -> sale.getType()
                                    .equals(TransactionType.Buy.toString()))
                            .mapToDouble(Sale::getValue)
                            .sum();
            double totalServerSold =
                    sales.stream()
                            .filter(sale -> sale.getType()
                                    .equals(TransactionType.Sell.toString()))
                            .mapToDouble(Sale::getValue)
                            .sum();

            for (Balance balance : balances) {
                UUID uuid = UUID.fromString(balance.getUserId());

                double totalBought =
                        sales.stream()
                                .filter(sale -> sale.getUser()
                                        .getUserId()
                                        .equals(uuid.toString()) && sale.getType()
                                        .equals(TransactionType.Buy.toString()))
                                .mapToDouble(Sale::getValue)
                                .sum();
                double totalSold =
                        sales.stream()
                                .filter(sale -> sale.getUser()
                                        .getUserId()
                                        .equals(uuid.toString()) && sale.getType()
                                        .equals(TransactionType.Sell.toString()))
                                .mapToDouble(Sale::getValue)
                                .sum();

                ItemStack playerHead = PlayerUtils.getPlayerHead(uuid);
                ItemMeta meta = playerHead.getItemMeta();

                List<Component> lore = ServerUtils.getLoreOrEmptyComponentList(meta);
                lore.add(Component.empty());
                lore.add(Component.text()
                        .content("Balance: ")
                        .append(Component.text(String.format("%s%s", ServerUtils.COIN_SYMBOL,
                                        ServerUtils.formatMoneyValue(balance.getValue())),
                                NamedTextColor.GOLD))
                        .build());
                lore.add(Component.text()
                        .content("Bought: ")
                        .append(Component.text(String.format("%s%s", ServerUtils.COIN_SYMBOL,
                                        ServerUtils.formatMoneyValue(totalBought)),
                                NamedTextColor.GREEN))
                        .build());
                lore.add(Component.text()
                        .content("Sold: ")
                        .append(Component.text(String.format("%s%s", ServerUtils.COIN_SYMBOL,
                                        ServerUtils.formatMoneyValue(totalSold)),
                                NamedTextColor.BLUE))
                        .build());
                meta.lore(lore);
                playerHead.setItemMeta(meta);

                inventory.addItem(playerHead);
            }

            ItemStack totalEconomy =
                    PlayerUtils.getPlayerHead(UUID.fromString("311deb92-9612-40da-992c-355d959d6513"), "Total Economy");
            ItemMeta meta = totalEconomy.getItemMeta();

            List<Component> lore = ServerUtils.getLoreOrEmptyComponentList(meta);
            lore.add(Component.empty());
            lore.add(Component.text()
                    .content("Total balances: ")
                    .append(Component.text(String.format("%s%s", ServerUtils.COIN_SYMBOL,
                                    ServerUtils.formatMoneyValue(totalEconomyValue)),
                            NamedTextColor.GOLD))
                    .build());
            lore.add(Component.text()
                    .content("Total bought: ")
                    .append(Component.text(String.format("%s%s", ServerUtils.COIN_SYMBOL,
                                    ServerUtils.formatMoneyValue(totalServerBought)),
                            NamedTextColor.GREEN))
                    .build());
            lore.add(Component.text()
                    .content("Total sold: ")
                    .append(Component.text(String.format("%s%s", ServerUtils.COIN_SYMBOL,
                                    ServerUtils.formatMoneyValue(totalServerSold)),
                            NamedTextColor.BLUE))
                    .build());
            meta.lore(lore);
            totalEconomy.setItemMeta(meta);

            inventory.setItem(inventory.getSize() - 1, totalEconomy);
        } catch (SQLException e) {
            System.out.println("Failed to load leaderboard: " + e.getMessage());
        }
    }

    @Override
    public void handleInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
