package dev.ricr.skyblock;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.database.Balance;
import dev.ricr.skyblock.utils.PlayerUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class LeaderBoardGUI implements InventoryHolder {
    @Getter
    private final Inventory inventory;
    private final SimpleSkyblock plugin;

    public LeaderBoardGUI(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Balance leaderboard"));

        Dao<Balance, String> balanceDao = this.plugin.databaseManager.getBalanceDao();

        try {
            List<Balance> balances = balanceDao.queryBuilder().orderBy("value", false).query();

            for (Balance balance : balances) {
                UUID uuid = UUID.fromString(balance.getUserId());
                ItemStack playerHead = PlayerUtils.getPlayerHead(uuid, balance.getValue());
                inventory.addItem(playerHead);
            }
        } catch (SQLException e) {
            // ignore for now
        }
    }
}
