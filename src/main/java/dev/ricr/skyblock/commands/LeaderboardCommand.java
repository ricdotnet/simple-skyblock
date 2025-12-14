package dev.ricr.skyblock.commands;

import dev.ricr.skyblock.gui.LeaderBoardGUI;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class LeaderboardCommand implements CommandExecutor {
    private final SimpleSkyblock plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] strings) {
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);
        player.sendMessage(Component.text("Loading balance leaderboard. This may take a moment", NamedTextColor.YELLOW));

        new LeaderBoardGUI(this.plugin, player);
        return true;
    }
}
