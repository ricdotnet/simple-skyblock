package dev.ricr.skyblock.commands;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class BalanceCommand implements CommandExecutor {
    private final SimpleSkyblock plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             String[] args) {
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);
        var playerEntity = this.plugin.onlinePlayers.getPlayer(player.getUniqueId()).getPlayerEntity();

        var message = String.format("<green>Your balance is <gold>%s", ServerUtils.formatMoneyValue(playerEntity.getBalance()));
        player.sendMessage(this.plugin.miniMessage.deserialize(message));

        return true;
    }
}
