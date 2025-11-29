package dev.ricr.skyblock.commands;

import dev.ricr.skyblock.gui.LeaderBoardGUI;
import dev.ricr.skyblock.SimpleSkyblock;
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by players");
            return true;
        }

        try {
            LeaderBoardGUI leaderBoardGUI = new LeaderBoardGUI(this.plugin);
            player.openInventory(leaderBoardGUI.getInventory());
        } catch (NullPointerException e) {
            player.sendMessage(Component.text("Failed to load leaderboard", NamedTextColor.RED));
            this.plugin.getLogger()
                    .severe("Failed to load leaderboard: " + e.getMessage());
        }

        return true;
    }
}
