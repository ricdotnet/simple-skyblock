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

        var playerRecord = this.plugin.onlinePlayers.getPlayer(player.getUniqueId());

        player.sendMessage(Component.text("Your balance is:", NamedTextColor.GREEN)
                .appendSpace()
                .append(
                        Component.text(String.format("%s", ServerUtils.formatMoneyValue(playerRecord.getBalance())),
                                NamedTextColor.GOLD))
        );

        return true;
    }
}
