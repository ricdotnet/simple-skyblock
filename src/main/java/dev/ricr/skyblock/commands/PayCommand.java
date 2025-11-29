package dev.ricr.skyblock.commands;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.Balance;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

@AllArgsConstructor
public class PayCommand implements CommandExecutor {
    private final SimpleSkyblock plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by players");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(Component.text("Usage: /pay <player> <amount>", NamedTextColor.YELLOW));
            return true;
        }

        String targetPlayerName = args[0];
        double amount = Double.parseDouble(args[1]);

        if (amount < 0) {
            player.sendMessage(Component.text("You are not allowed to pay negative money", NamedTextColor.RED));
            return true;
        }

        Server server = plugin.getServer();
        Player targetPlayer = server.getPlayer(args[0]);

        if (targetPlayer == null) {
            player.sendMessage(Component.text("Player not found", NamedTextColor.RED));
            return true;
        }

        Dao<Balance, String> balanceDao = plugin.databaseManager.getBalancesDao();

        try {
            Balance targetPlayerBalance = balanceDao.queryForId(targetPlayer.getUniqueId()
                    .toString());

            if (player.isOp()) {
                targetPlayerBalance.setValue(targetPlayerBalance.getValue() + amount);
                balanceDao.update(targetPlayerBalance);
            } else {
                Balance senderBalance = balanceDao.queryForId(player.getUniqueId()
                        .toString());

                if (senderBalance.getValue() >= amount) {
                    senderBalance.setValue(senderBalance.getValue() - amount);
                    targetPlayerBalance.setValue(targetPlayerBalance.getValue() + amount);
                } else {
                    player.sendMessage(Component.text("You don't have enough money to pay that amount",
                            NamedTextColor.RED));
                    return true;
                }

                balanceDao.update(senderBalance);
                balanceDao.update(targetPlayerBalance);
            }
        } catch (SQLException e) {
            // ignore for now
        }

        player.sendMessage(Component.text(String.format("Paid %s%s to %s", ServerUtils.COIN_SYMBOL,
                ServerUtils.formatMoneyValue(amount),
                targetPlayerName), NamedTextColor.GREEN));
        targetPlayer.sendMessage(Component.text(String.format("You received %s%s from %s",
                ServerUtils.COIN_SYMBOL, ServerUtils.formatMoneyValue(amount), player.getName()),
                NamedTextColor.GREEN));

        return true;
    }
}
