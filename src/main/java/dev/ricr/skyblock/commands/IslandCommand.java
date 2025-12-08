package dev.ricr.skyblock.commands;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.Island;
import dev.ricr.skyblock.database.IslandUserTrustLink;
import dev.ricr.skyblock.database.User;
import dev.ricr.skyblock.gui.IslandGUI;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.codehaus.plexus.util.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

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
        Dao<User, String> usersDao = this.plugin.databaseManager.getUsersDao();

        // for adding trusted players
        if (args.length == 2 && args[0].equalsIgnoreCase("trust")) {
            Player targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                sender.sendMessage(Component.text(String.format("Player %s is not online", args[1]), NamedTextColor.RED));
                return true;
            }

            try {
                Island userIsland = islandsDao.queryForId(player.getUniqueId().toString());
                User targetUser = usersDao.queryForId(targetPlayer.getUniqueId().toString());

                if (targetUser == null) {
                    sender.sendMessage(Component.text(String.format("Player %s does not exist in our server database", args[1]), NamedTextColor.RED));
                    return true;
                }

                ForeignCollection<IslandUserTrustLink> trustedPlayers = userIsland.getTrustedPlayers();

                IslandUserTrustLink islandUserTrustLink = new IslandUserTrustLink();
                islandUserTrustLink.setIsland(userIsland);
                islandUserTrustLink.setUser(targetUser);

                trustedPlayers.add(islandUserTrustLink);
                userIsland.setTrustedPlayers(trustedPlayers);

                islandsDao.update(userIsland);

                // Add to island record live list
                this.plugin.islandManager.getIslandRecord(player.getUniqueId())
                        .trustedPlayers()
                        .add(targetPlayer.getUniqueId().toString());

                player.sendMessage(Component.text(String.format("Player %s is now trusted", targetPlayer.getName()), NamedTextColor.GREEN));
            } catch (SQLException e) {
                // ignore for now
            }
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("create")) {
            this.createPlayerIslandWorld(player);
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("delete")) {
            this.deletePlayerIslandWorld(player);
        }

        return true;
    }

    private void createPlayerIslandWorld(Player player) {
        String islandName = String.format("islands/%s", player.getUniqueId());

        WorldCreator worldCreator = new WorldCreator(islandName);
        worldCreator.generator("SimpleSkyblock");
        // TODO: generate custom seed #

        World newIsland = Bukkit.createWorld(worldCreator);

        if (newIsland == null) {
            player.sendMessage(Component.text("Unable to create a new island", NamedTextColor.RED));
        } else {
            newIsland.save();

            Location newLocation = this.plugin.islandGenerator.generateIsland(newIsland, player);
            this.createIslandDatabaseRecord(player);

            newLocation.setY(64);
            newLocation.setX(0.5);
            newLocation.setZ(2.5);
            newLocation.setYaw(180);

            player.teleport(newLocation);
            player.sendMessage(Component.text("Welcome to your new island", NamedTextColor.GREEN));
        }
    }

    private void createIslandDatabaseRecord(Player player) {
        String playerUniqueId = player.getUniqueId()
                .toString();

        Dao<User, String> userDao = this.plugin.databaseManager.getUsersDao();
        Dao<Island, String> islandsDao = this.plugin.databaseManager.getIslandsDao();

        try {
            User user = userDao.queryForId(playerUniqueId);
            if (user == null) {
                return;
            }
            Island island = islandsDao.queryForId(user.getUserId());
            if (island != null) {
                this.plugin.getLogger()
                        .info(String.format("Player %s already has an island and tried creating another one",
                                player.getName()));
                return;
            }

            island = new Island();
            island.setId(user.getUserId());
            island.setUser(user);

            // Using multiple island worlds means we always start at 0 64 0
            island.setPositionX(0.0d);
            island.setPositionZ(0.0d);

            islandsDao.create(island);
        } catch (SQLException e) {
            // ignore for now
        }
    }

    private void deletePlayerIslandWorld(Player player) {
        String islandName = String.format("islands/%s", player.getUniqueId());

        World islandWorld = Bukkit.getWorld(islandName);
        if (islandWorld == null) {
            player.sendMessage(Component.text("Unable to delete your island because it does not exist", NamedTextColor.RED));
            return;
        }

        World lobbyWorld = Bukkit.getWorld("lobby");
        assert lobbyWorld != null;
        player.teleport(new Location(lobbyWorld, 0.5, 65, 0.5));

        Bukkit.unloadWorld(islandWorld, false);

        File islandWorldFolder = islandWorld.getWorldFolder();
        try {
            FileUtils.deleteDirectory(islandWorldFolder);
        } catch (IOException e) {
            // ignore for now
            player.sendMessage(Component.text("Unable to delete your island", NamedTextColor.RED));
            return;
        }

        this.deleteIslandDatabaseRecord(player);
        player.sendMessage(Component.text("Successfully deleted your island", NamedTextColor.GREEN));
    }

    private void deleteIslandDatabaseRecord(Player player) {
        String playerUniqueId = player.getUniqueId()
                .toString();

        Dao<Island, String> islandsDao = this.plugin.databaseManager.getIslandsDao();

        try {
            Island island = islandsDao.queryForId(playerUniqueId);
            if (island == null) {
                this.plugin.getLogger()
                        .info(String.format("Player %s does not have an island database record to delete",
                                player.getName()));
                return;
            }

            islandsDao.delete(island);
        } catch (SQLException e) {
            // ignore for now
        }
    }
}
