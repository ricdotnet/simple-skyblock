package dev.ricr.skyblock.commands;

import dev.ricr.skyblock.gui.LeaderBoardGUI;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.utils.ServerUtils;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.List;


public class LeaderboardCommand implements BasicCommand {
    private final SimpleSkyblock plugin;

    public LeaderboardCommand(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.plugin.registerCommand("leaderboard", List.of("baltop", "balancetop"), this);
    }

    @Override
    public void execute(CommandSourceStack commandSourceStack, String[] args) {
        var sender = commandSourceStack.getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        player.sendMessage("Loading balance leaderboard, this could take a moment.");

        new LeaderBoardGUI(this.plugin, player);
    }
}
