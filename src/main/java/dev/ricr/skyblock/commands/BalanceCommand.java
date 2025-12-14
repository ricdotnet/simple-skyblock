package dev.ricr.skyblock.commands;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.User;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

@AllArgsConstructor
public class BalanceCommand implements CommandExecutor {
    private final SimpleSkyblock plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             String[] args) {
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var playerRecord = this.plugin.onlinePlayers.getPlayer(player.getUniqueId());

        player.sendMessage(Component.text(String.format("Your balance is: %s%s", ServerUtils.COIN_SYMBOL,
                ServerUtils.formatMoneyValue(playerRecord.getBalance())), NamedTextColor.GOLD));

        return true;
    }
}
