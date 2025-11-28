package dev.ricr.skyblock.commands;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.gui.GambleSessionGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class GambleCommand implements CommandExecutor {

    private final SimpleSkyblock plugin;
    private final Map<UUID, GambleSessionGUI> gambleSessions;

    public GambleCommand(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.gambleSessions = new HashMap<>();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("To start a gamble session, type /gamble <amount>. To join a session, type /gamble " + "<player>");
            return true;
        }

        Double amount = null;
        String host = null;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            // we then assume the user wants to join a session
            host = args[0];
        }

        if (null != amount) {
            this.plugin.getLogger()
                    .info("Starting gamble session for " + player.getName());

            if (amount <= 0) {
                player.sendMessage("You cannot gamble negative money");
                return true;
            }

            if (gambleSessions.containsKey(player.getUniqueId())) {
                player.sendMessage(String.format("You already have an ongoing gamble session. Type /gamble %s to " +
                        "join", player.getName()));
                return true;
            }

            GambleSessionGUI gambleSession = new GambleSessionGUI(this.plugin, player.getUniqueId(), amount);
            gambleSessions.put(player.getUniqueId(), gambleSession);

            player.openInventory(gambleSession.getInventory());

            this.plugin.getServer()
                    .sendMessage(Component.text(String.format("%s started a gamble session. Do /gamble %s to join",
                            player.getName(), player.getName()), NamedTextColor.GREEN));

            this.initiateGambleSessionCountdown(gambleSession);

            return true;
        } else if (host != null) {
            Player playerHost = Bukkit.getPlayer(args[0]);
            if (playerHost == null) {
                player.sendMessage("No gamble session found for player " + host);
                return true;
            }

            GambleSessionGUI gambleSession = gambleSessions.get(playerHost.getUniqueId());

            if (gambleSession == null) {
                player.sendMessage("No gamble session found for player " + host);
                return true;
            }

            if (gambleSession.getHost() == Objects.requireNonNull(playerHost.getUniqueId())) {
                player.sendMessage("You rejoined your own gamble session.");
            } else {
                gambleSession.addPlayer(playerHost.getUniqueId());
                player.sendMessage("You joined the gamble session of " + playerHost.getName());
            }

            player.openInventory(gambleSession.getInventory());
        }

        return true;
    }

    private void initiateGambleSessionCountdown(GambleSessionGUI gambleSession) {
        Bukkit.getAsyncScheduler()
                .runAtFixedRate(plugin, (task) -> {
                    long remaining = gambleSession.getCountdownClock()
                            .decrementAndGet();

                    if (remaining <= 0) {
                        task.cancel();

                        Bukkit.getGlobalRegionScheduler()
                                .execute(plugin, () -> {
                                    gambleSession.chooseWinner();
                                    gambleSessions.remove(gambleSession.getHost());
                                });
                        return;
                    }

                    Bukkit.getGlobalRegionScheduler()
                            .execute(plugin, () -> gambleSession.updateCountdownClock((int) remaining));
                }, 1, 1, TimeUnit.SECONDS);
    }
}
