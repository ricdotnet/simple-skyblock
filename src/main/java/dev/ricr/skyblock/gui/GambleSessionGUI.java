package dev.ricr.skyblock.gui;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class GambleSessionGUI implements InventoryHolder {

    private final SimpleSkyblock plugin;
    private final UUID host;
    private final Set<UUID> players = new LinkedHashSet<>();
    private double amount;

    private Inventory inventory;

    private final AtomicInteger countdownClock = new AtomicInteger(60);

    public GambleSessionGUI(SimpleSkyblock plugin, UUID host, double amount) {
        this.plugin = plugin;
        this.host = host;
        this.amount = amount;
        this.inventory = Bukkit.createInventory(this, 27, Component.text(String.format("Gamble session: %s%s",
                ServerUtils.COIN_SYMBOL, amount)));

        this.addPlayer(host);
        this.updateCountdownClock(countdownClock.get());
    }

    public void addPlayer(UUID player) {
        this.players.add(player);
        this.refreshInventory();
    }

    public void removePlayer(UUID player) {
        this.players.remove(player);
        this.refreshInventory();
    }

    public void chooseWinner() {
        this.plugin.getServer()
                .sendMessage(Component.text("Choosing a winner..."));
        this.plugin.getServer()
                .sendMessage(Component.text("Winner: " + players.iterator()
                        .next()));
    }

    public void refreshInventory() {
        inventory.clear();

        int slot = 0;
        for (UUID playerId : players) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(playerId);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(op);
            meta.displayName(Component.text(Objects.requireNonNull(op.getName())));
            head.setItemMeta(meta);

            inventory.setItem(slot++, head);
        }
    }

    public void updateCountdownClock(int seconds) {
        ItemStack clock = new ItemStack(Material.CLOCK, seconds);

        inventory.setItem(26, clock); // bottom-right slot
    }

}
