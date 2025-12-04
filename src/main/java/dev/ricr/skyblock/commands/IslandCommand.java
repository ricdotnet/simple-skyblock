package dev.ricr.skyblock.commands;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.Island;
import dev.ricr.skyblock.gui.IslandGUI;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
public class IslandCommand implements CommandExecutor {
    private final SimpleSkyblock plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by players");
            return true;
        }

        if (args.length == 0) {
            IslandGUI islandGUI = new IslandGUI(this.plugin, player);
            player.openInventory(islandGUI.getInventory());
            return true;
        }

        Dao<Island, String> islandsDao = this.plugin.databaseManager.getIslandsDao();

        // for adding trusted players
        if (args.length == 2 && args[0].equalsIgnoreCase("trust")) {
            Player targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                sender.sendMessage(Component.text(String.format("Player %s is not online", args[1]), NamedTextColor.RED));
                return true;
            }

            try {
                Island userIsland = islandsDao.queryForId(player.getUniqueId().toString());

                Set<UUID> trustedPlayers = userIsland.getTrustedPlayers();
                trustedPlayers.add(targetPlayer.getUniqueId());
                userIsland.setTrustedPlayers(trustedPlayers);

                islandsDao.update(userIsland);

                // Add to island record live list
                this.plugin.islandManager.getIslandRecord(player.getUniqueId()).trustedPlayers().add(targetPlayer.getUniqueId());

                player.sendMessage(Component.text(String.format("Player %s is now trusted", targetPlayer.getName()), NamedTextColor.GREEN));
            } catch (SQLException e) {
                // ignore for now
            }
        }

        return true;
    }
}
