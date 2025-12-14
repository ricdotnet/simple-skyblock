package dev.ricr.skyblock.commands;

import dev.ricr.skyblock.SimpleSkyblock;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class LobbyCommand implements CommandExecutor {
    private final SimpleSkyblock plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by players");
            return true;
        }

        World lobbyWorld = Bukkit.getWorld("lobby");

        if (lobbyWorld == null) {
            player.sendMessage(Component.text("The lobby world could not be found", NamedTextColor.RED));
            return true;
        }

        Location spawnLocation = new Location(lobbyWorld, 0.5, 65, 0.5);
        player.teleport(spawnLocation);

        return true;
    }
}
