package dev.ricr.skyblock.commands;

import dev.ricr.skyblock.LeaderBoardGUI;
import dev.ricr.skyblock.SimpleSkyblock;
import lombok.AllArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class LeaderboardCommand implements CommandExecutor {
    private final SimpleSkyblock plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] strings) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by players");
            return true;
        }

        LeaderBoardGUI leaderBoardGUI = new LeaderBoardGUI(this.plugin);
        player.openInventory(leaderBoardGUI.getInventory());

        return false;
    }
}
