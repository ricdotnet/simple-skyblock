package dev.ricr.skyblock.gui;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.TransactionEntity;
import dev.ricr.skyblock.database.PlayerEntity;
import dev.ricr.skyblock.enums.TransactionType;
import dev.ricr.skyblock.utils.InventoryUtils;
import dev.ricr.skyblock.utils.PlayerUtils;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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

    public LeaderBoardGUI(SimpleSkyblock plugin, Player player) {
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Balance leaderboard"));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Dao<PlayerEntity, String> playersDao = plugin.databaseManager.getPlayersDao();
            Dao<TransactionEntity, Integer> saleDao = plugin.databaseManager.getTransactionsDao();

            try {
                List<PlayerEntity> playerEntities = playersDao.queryBuilder()
                        .orderBy("balance", false)
                        .query();
                List<TransactionEntity> sales = saleDao.queryForAll();

                double totalEconomyValue = playerEntities.stream()
                        .mapToDouble(PlayerEntity::getBalance)
                        .sum();
                double totalServerBought =
                        sales.stream()
                                .filter(sale -> sale.getType()
                                        .equals(TransactionType.ShopBuy.toString()))
                                .mapToDouble(TransactionEntity::getPrice)
                                .sum();
                double totalServerSold =
                        sales.stream()
                                .filter(sale -> sale.getType()
                                        .equals(TransactionType.ShopSell.toString()))
                                .mapToDouble(TransactionEntity::getPrice)
                                .sum();

                for (PlayerEntity playerEntity : playerEntities) {
                    UUID uuid = UUID.fromString(playerEntity.getPlayerId());

                    double totalBought =
                            sales.stream()
                                    .filter(sale -> sale.getPlayer()
                                            .getPlayerId()
                                            .equals(uuid.toString()) && sale.getType()
                                            .equals(TransactionType.ShopBuy.toString()))
                                    .mapToDouble(TransactionEntity::getPrice)
                                    .sum();
                    double totalSold =
                            sales.stream()
                                    .filter(sale -> sale.getPlayer()
                                            .getPlayerId()
                                            .equals(uuid.toString()) && sale.getType()
                                            .equals(TransactionType.ShopSell.toString()))
                                    .mapToDouble(TransactionEntity::getPrice)
                                    .sum();

                    ItemStack playerHead = PlayerUtils.getPlayerHead(uuid);
                    ItemMeta meta = playerHead.getItemMeta();

                    List<Component> lore = ServerUtils.getLoreOrEmptyComponentList(meta);
                    lore.add(Component.empty());
                    lore.add(plugin.miniMessage.deserialize("<!italic><white>Balance: <color:#F23CC7A><balance>",
                            Placeholder.unparsed("balance", ServerUtils.formatMoneyValue(playerEntity.getBalance()))
                    ));
                    lore.add(plugin.miniMessage.deserialize("<!italic><white>Bought: <color:#F23CC7A><total_bought>",
                            Placeholder.unparsed("total_bought", ServerUtils.formatMoneyValue(totalBought))
                    ));
                    lore.add(plugin.miniMessage.deserialize("<!italic><white>Sold: <color:#F23CC7A><total_sold>",
                            Placeholder.unparsed("total_sold", ServerUtils.formatMoneyValue(totalSold))
                    ));
                    meta.lore(lore);
                    playerHead.setItemMeta(meta);

                    this.inventory.addItem(playerHead);
                }

                ItemStack totalEconomy =
                        PlayerUtils.getPlayerHead(UUID.fromString("311deb92-9612-40da-992c-355d959d6513"), "Total Economy");
                ItemMeta meta = totalEconomy.getItemMeta();

                List<Component> lore = ServerUtils.getLoreOrEmptyComponentList(meta);
                lore.add(Component.empty());
                lore.add(plugin.miniMessage.deserialize("<!italic><white>Total balances: <color:#F23CC7A><total_economy>",
                        Placeholder.unparsed("total_economy", ServerUtils.formatMoneyValue(totalEconomyValue))
                ));
                lore.add(plugin.miniMessage.deserialize("<!italic><white>Total bought: <color:#F23CC7A><total_server_bought>",
                        Placeholder.unparsed("total_server_bought", ServerUtils.formatMoneyValue(totalServerBought))
                ));
                lore.add(plugin.miniMessage.deserialize("<!italic><white>Total sold: <color:#F23CC7A><total_server_sold>",
                        Placeholder.unparsed("total_server_sold", ServerUtils.formatMoneyValue(totalServerSold))
                ));
                meta.lore(lore);
                totalEconomy.setItemMeta(meta);

                this.inventory.setItem(inventory.getSize() - 1, totalEconomy);
                InventoryUtils.fillEmptySlots(this.inventory);

                // Open async to allow all players to load without blocking the main thread with db operations
                Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(this.getInventory()));
            } catch (SQLException e) {
                player.sendMessage(plugin.miniMessage.deserialize("<red>Failed to load leaderboard"));
            }
        });
    }

    @Override
    public void handleInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
