package dev.ricr.skyblock.gui;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.Balance;
import dev.ricr.skyblock.database.Gamble;
import dev.ricr.skyblock.enums.GambleType;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class GambleSessionGUI implements InventoryHolder {

    private final SimpleSkyblock plugin;
    private final Player host;
    private final Set<Player> players = new LinkedHashSet<>();
    private final double originalAmount;
    private double amount;

    private Inventory inventory;

    private final AtomicInteger countdownClock = new AtomicInteger(ServerUtils.GAMBLE_COUNTDOWN);

    public GambleSessionGUI(SimpleSkyblock plugin, Player host, double amount) {
        this.plugin = plugin;
        this.host = host;
        this.originalAmount = amount;
        this.amount = amount;
        this.inventory = Bukkit.createInventory(this, 27, Component.text(String.format("Gamble session: %s%s",
                ServerUtils.COIN_SYMBOL, amount)));

        this.addPlayer(host);
        this.updateCountdownClock(countdownClock.get());
    }

    public void addPlayer(Player player) {
        this.players.add(player);
        this.amount += amount;

        this.updatePlayerBalance(player, -originalAmount);

        this.refreshInventory();
    }

    // TODO: implement later to allow quit a gamble
//    public void removePlayer(UUID player) {
//        this.players.remove(player);
//        this.amount -= amount;
//        this.refreshInventory();
//    }

    public void chooseWinner() {
        if (this.players.size() == 1) {
            Player hostPlayer = this.players.iterator()
                    .next();

            hostPlayer.sendMessage(Component.text("Your gamble session has been voided, no other players joined.",
                    NamedTextColor.YELLOW));

            updatePlayerBalance(hostPlayer, this.originalAmount);
            this.inventory.close();

            return;
        }

        int randomIndex = new Random().nextInt(players.size());
        Player winner = players.toArray(Player[]::new)[randomIndex];

        Dao<Balance, String> balancesDao = this.plugin.databaseManager.getBalancesDao();
        Gamble gamble = new Gamble();

        for (Player player : players) {
            if (player.getUniqueId() == winner.getUniqueId()) {
                player.sendMessage(Component.text("You won the gamble!", NamedTextColor.GREEN));

                try {
                    Balance balance = balancesDao.queryForId(player.getUniqueId()
                            .toString());
                    gamble.setUser(balance);
                    gamble.setAmount(this.amount);
                    gamble.setType(GambleType.Won.toString());
                } catch (SQLException e) {
                    // ignore for now
                }

                updatePlayerBalance(player, this.amount);
            } else {
                player.sendMessage(Component.text("You lost the gamble!", NamedTextColor.RED));

                try {
                    Balance balance = balancesDao.queryForId(player.getUniqueId()
                            .toString());
                    gamble.setUser(balance);
                    gamble.setAmount(this.originalAmount);
                    gamble.setType(GambleType.Lost.toString());
                } catch (SQLException e) {
                    // ignore for now
                }
            }
        }

        try {
            Dao<Gamble, Integer> gamblesDao = this.plugin.databaseManager.getGamblesDao();
            gamblesDao.create(gamble);
        } catch (SQLException e) {
            // ignore for now
        }

        this.inventory.close();
    }

    public void refreshInventory() {
        inventory.clear();

        int slot = 0;
        for (Player player : players) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(player);
            meta.displayName(Component.text(player.getName(), NamedTextColor.YELLOW));
            head.setItemMeta(meta);

            inventory.setItem(slot++, head);
        }
    }

    public void updateCountdownClock(int seconds) {
        ItemStack clock = new ItemStack(Material.CLOCK, seconds);

        inventory.setItem(26, clock); // bottom-right slot
    }

    private void updatePlayerBalance(Player player, double amount) {
        try {
            Dao<Balance, String> balanceDao = this.plugin.databaseManager.getBalancesDao();
            Balance balance = balanceDao.queryForId(player.getUniqueId()
                    .toString());
            balance.setValue(balance.getValue() + amount);
            balanceDao.update(balance);
        } catch (SQLException e) {
            // ignore for now
        }
    }
}
