package dev.ricr.skyblock.gui;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.DatabaseChange;
import dev.ricr.skyblock.database.GambleEntity;
import dev.ricr.skyblock.enums.GambleOutcome;
import dev.ricr.skyblock.enums.SoundType;
import dev.ricr.skyblock.utils.InventoryUtils;
import dev.ricr.skyblock.utils.PlayerUtils;
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

    private final Inventory inventory;

    private final AtomicInteger countdownClock = new AtomicInteger(ServerUtils.GAMBLE_COUNTDOWN);

    public GambleSessionGUI(SimpleSkyblock plugin, Player host, double amount) {
        this.plugin = plugin;
        this.host = host;
        this.originalAmount = amount;
        this.inventory = Bukkit.createInventory(this, 27, Component.text(String.format("Gamble session - %s",
                this.host.getName())));

        this.addPlayer(host);

        InventoryUtils.fillEmptySlots(inventory);
    }

    public void addPlayer(Player player) {
        this.players.add(player);
        this.amount += this.originalAmount;

        this.updatePlayerBalance(player, -originalAmount);

        this.refreshInventory();
    }

    public void chooseWinner() {
        if (this.players.size() == 1) {
            Player hostPlayer = this.players.iterator()
                    .next();

            hostPlayer.sendMessage(Component.text("Your gamble session has been voided, no other players joined.",
                    NamedTextColor.YELLOW));

            var voidedMessage = "<gold>No winner, gamble session voided.";
            PlayerUtils.showTitleMessage(this.plugin, hostPlayer, this.plugin.miniMessage.deserialize(voidedMessage));

            updatePlayerBalance(hostPlayer, this.originalAmount);
            this.inventory.close();

            return;
        }

        int randomIndex = new Random().nextInt(players.size());
        Player winner = players.toArray(Player[]::new)[randomIndex];

        for (Player player : players) {
            var gamble = new GambleEntity();
            var playerEntity = this.plugin.onlinePlayers.getPlayer(player.getUniqueId()).getPlayerEntity();

            if (player.getUniqueId() == winner.getUniqueId()) {
                var message = Component.text("You won the gamble!", NamedTextColor.GREEN);

                player.sendMessage(message);
                PlayerUtils.showTitleMessage(this.plugin, player, message);

                gamble.setPlayer(playerEntity);
                gamble.setAmount(this.amount);
                gamble.setType(GambleOutcome.Won.toString());

                updatePlayerBalance(player, this.amount);

                PlayerUtils.playSound(player, SoundType.POSITIVE);
            } else {
                var message = Component.text("You lost the gamble \uD83E\uDD40", NamedTextColor.RED);

                player.sendMessage(message);
                PlayerUtils.showTitleMessage(this.plugin, player, message);
                player.sendMessage(Component.text(String.format("%s won this gamble session", winner.getName()),
                        NamedTextColor.DARK_RED));

                gamble.setPlayer(playerEntity);
                gamble.setAmount(this.originalAmount);
                gamble.setType(GambleOutcome.Lost.toString());

                PlayerUtils.playSound(player, SoundType.NEGATIVE);
            }

            var gambleRecordAdd = new DatabaseChange.GambleRecordAdd(gamble);
            this.plugin.databaseChangesAccumulator.add(gambleRecordAdd);
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

    private void updatePlayerBalance(Player player, double amount) {
        var hostPlayerEntity = this.plugin.onlinePlayers.getPlayer(player.getUniqueId()).getPlayerEntity();
        hostPlayerEntity.setBalance(hostPlayerEntity.getBalance() + amount);

        var playerCreateOrUpdate = new DatabaseChange.PlayerCreateOrUpdate(hostPlayerEntity);
        this.plugin.databaseChangesAccumulator.add(playerCreateOrUpdate);
    }
}
