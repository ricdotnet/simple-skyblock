package dev.ricr.skyblock.gui;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.DatabaseChange;
import dev.ricr.skyblock.database.GambleEntity;
import dev.ricr.skyblock.database.PlayerEntity;
import dev.ricr.skyblock.enums.GambleType;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

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

    private final BossBar bossBar;
    private final Inventory inventory;

    private final AtomicInteger countdownClock = new AtomicInteger(ServerUtils.GAMBLE_COUNTDOWN);

    public GambleSessionGUI(SimpleSkyblock plugin, Player host, double amount) {
        this.plugin = plugin;
        this.host = host;
        this.originalAmount = amount;
        this.inventory = Bukkit.createInventory(this, 27, Component.text(String.format("Gamble session - %s",
                this.host.getName())));

        this.bossBar = Bukkit.createBossBar(String.format("Gamble end in %ss - Money pool: %s%s",
                        this.countdownClock.get(), ServerUtils.COIN_SYMBOL, ServerUtils.formatMoneyValue(this.amount)),
                BarColor.GREEN, BarStyle.SOLID);
        this.bossBar.setProgress(1.0);
        this.bossBar.setVisible(true);

        this.addPlayer(host);

        this.updateCountdownClock(countdownClock.get());
    }

    public void addPlayer(Player player) {
        this.players.add(player);
        this.amount += this.originalAmount;

        this.updatePlayerBalance(player, -originalAmount);

        this.bossBar.setColor(BarColor.GREEN);
        this.bossBar.addPlayer(player);

        this.refreshInventory();
    }

    public void chooseWinner() {
        if (this.players.size() == 1) {
            Player hostPlayer = this.players.iterator()
                    .next();

            hostPlayer.sendMessage(Component.text("Your gamble session has been voided, no other players joined.",
                    NamedTextColor.YELLOW));

            updatePlayerBalance(hostPlayer, this.originalAmount);
            this.bossBar.removeAll();
            this.inventory.close();

            return;
        }

        int randomIndex = new Random().nextInt(players.size());
        Player winner = players.toArray(Player[]::new)[randomIndex];

        for (Player player : players) {
            var gamble = new GambleEntity();
            var playerRecord = this.plugin.onlinePlayers.getPlayer(player.getUniqueId());

            if (player.getUniqueId() == winner.getUniqueId()) {
                player.sendMessage(Component.text("You won the gamble!", NamedTextColor.GREEN));

                gamble.setPlayer(playerRecord);
                gamble.setAmount(this.amount);
                gamble.setType(GambleType.Won.toString());

                updatePlayerBalance(player, this.amount);

                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1f, 1f);
            } else {
                player.sendMessage(Component.text("You lost the gamble \uD83E\uDD40", NamedTextColor.RED));
                player.sendMessage(Component.text(String.format("%s won this gamble session", winner.getName()),
                        NamedTextColor.DARK_RED));

                gamble.setPlayer(playerRecord);
                gamble.setAmount(this.originalAmount);
                gamble.setType(GambleType.Lost.toString());

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1f, 1f);
            }

            var gambleRecordAdd = new DatabaseChange.GambleRecordAdd(gamble);
            this.plugin.databaseChangesAccumulator.add(gambleRecordAdd);
        }

        this.bossBar.removeAll();
        this.bossBar.setVisible(false);
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
        this.bossBar.setTitle(String.format("Gamble end in %ss - Money pool: %s%s",
                seconds, ServerUtils.COIN_SYMBOL, ServerUtils.formatMoneyValue(this.amount)));
        this.bossBar.setProgress(1.0 - seconds / (double) ServerUtils.GAMBLE_COUNTDOWN);
    }

    private void updatePlayerBalance(Player player, double amount) {
        PlayerEntity hostPlayer = this.plugin.onlinePlayers.getPlayer(player.getUniqueId());
        hostPlayer.setBalance(hostPlayer.getBalance() + amount);

        var playerCreateOrUpdate = new DatabaseChange.PlayerCreateOrUpdate(hostPlayer);
        this.plugin.databaseChangesAccumulator.add(playerCreateOrUpdate);
    }
}
