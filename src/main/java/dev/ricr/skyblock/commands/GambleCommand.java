package dev.ricr.skyblock.commands;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.Balance;
import dev.ricr.skyblock.gui.GambleSessionGUI;
import dev.ricr.skyblock.utils.ServerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
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
            player.sendMessage(Component.text()
                    .content("To start a gamble session, type /gamble <amount>")
                    .color(NamedTextColor.RED)
                    .append(Component.newline())
                    .append(Component.text("To join a session, type /gamble <player>", NamedTextColor.AQUA))
            );
            return true;
        }

        Dao<Balance, String> balanceDao = this.plugin.databaseManager.getBalancesDao();

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
                player.sendMessage(Component.text("You cannot gamble negative money", NamedTextColor.RED));
                return true;
            }

            if (amount < ServerUtils.GAMBLE_MINIMUM_BALANCE || amount > ServerUtils.GAMBLE_MAXIMUM_BALANCE) {
                player.sendMessage(Component.text(String.format("You can only gamble more than %s%s and less than %s%s",
                        ServerUtils.COIN_SYMBOL, ServerUtils.GAMBLE_MINIMUM_BALANCE, ServerUtils.COIN_SYMBOL,
                        ServerUtils.GAMBLE_MAXIMUM_BALANCE), NamedTextColor.RED));
                return true;
            }

            try {
                Balance playerBalance = balanceDao.queryForId(player.getUniqueId()
                        .toString());

                if (playerBalance.getValue() < amount) {
                    player.sendMessage(Component.text(String.format("Your balance is less than you tried to gamble " +
                                    "for %s%s",
                            ServerUtils.COIN_SYMBOL, ServerUtils.formatMoneyValue(amount)), NamedTextColor.RED));

                    return true;
                }
            } catch (SQLException e) {
                // ignore for now
            }


            if (gambleSessions.containsKey(player.getUniqueId())) {
                player.sendMessage(Component.text(String.format("You already have an ongoing gamble session. Type " +
                        "/gamble %s to " +
                        "join", player.getName()), NamedTextColor.GREEN));

                return true;
            }

            GambleSessionGUI gambleSession = new GambleSessionGUI(this.plugin, player, amount);
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
                player.sendMessage(Component.text(String.format("No gamble session found for player %s", host),
                        NamedTextColor.RED));
                return true;
            }

            GambleSessionGUI gambleSession = gambleSessions.get(playerHost.getUniqueId());

            if (gambleSession == null) {
                player.sendMessage(Component.text(String.format("No gamble session found for player %s", host),
                        NamedTextColor.RED));
                return true;
            }

            if (gambleSession.getPlayers()
                    .size() == 5) {
                player.sendMessage(Component.text("The gamble session is full", NamedTextColor.RED));
                return true;
            }

            if (gambleSession.getHost()
                    .getUniqueId() == player.getUniqueId()) {
                player.sendMessage(Component.text("You rejoined your own gamble session", NamedTextColor.YELLOW));
            } else {

                try {
                    Balance playerBalance = balanceDao.queryForId(player.getUniqueId()
                            .toString());

                    if (playerBalance.getValue() < gambleSession.getAmount()) {
                        player.sendMessage(Component.text(String.format("You cannot join a gamble session with less " +
                                                "than %s%s",
                                        ServerUtils.COIN_SYMBOL,
                                        ServerUtils.formatMoneyValue(gambleSession.getAmount())),
                                NamedTextColor.RED));
                        return true;
                    }
                } catch (SQLException e) {
                    // ignore for now
                }

                gambleSession.addPlayer(player);
                player.sendMessage(Component.text(String.format("You joined the gamble session of %s",
                                playerHost.getName()),
                        NamedTextColor.YELLOW));
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
                                    gambleSessions.remove(gambleSession.getHost()
                                            .getUniqueId());
                                });
                        return;
                    }

                    Bukkit.getGlobalRegionScheduler()
                            .execute(plugin, () -> gambleSession.updateCountdownClock((int) remaining));
                }, 1, 1, TimeUnit.SECONDS);
    }
}
