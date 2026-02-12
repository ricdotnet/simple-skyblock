package dev.ricr.skyblock.commands;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.utils.ServerUtils;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;

public class LobbyCommand implements BasicCommand {
    private final SimpleSkyblock plugin;

    public LobbyCommand(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.plugin.registerCommand("lobby", List.of("spawn"), this);
    }

    @Override
    public void execute(CommandSourceStack commandSourceStack, String[] args) {
        var sender = commandSourceStack.getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        World lobbyWorld = this.plugin.worldManager.load("lobby");

        if (lobbyWorld == null) {
            player.sendMessage(this.plugin.miniMessage.deserialize("<red>The lobby world could not be found"));
            return;
        }

        Location spawnLocation = new Location(lobbyWorld, 0.5, 65, 0.5);
        player.teleport(spawnLocation);
    }
}
