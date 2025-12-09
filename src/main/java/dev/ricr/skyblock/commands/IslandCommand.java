package dev.ricr.skyblock.commands;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.Island;
import dev.ricr.skyblock.database.IslandUserTrustLink;
import dev.ricr.skyblock.database.User;
import dev.ricr.skyblock.gui.IslandGUI;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class IslandCommand {
    private final SimpleSkyblock plugin;

    private final Dao<User, String> usersDao;
    private final Dao<Island, String> islandsDao;

    public IslandCommand(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.usersDao = plugin.databaseManager.getUsersDao();
        this.islandsDao = plugin.databaseManager.getIslandsDao();
    }

    public void register() {
        this.plugin.getLifecycleManager()
                .registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
                    LiteralCommandNode<CommandSourceStack> island = this.command();

                    commands.registrar().register(island);
                    commands.registrar().register(Commands.literal("is")
                            .redirect(island)
                            .build()
                    );
                });
    }

    private LiteralCommandNode<CommandSourceStack> command() {
        return Commands.literal("island")
                .executes(this::openIslandGUI)
                .then(Commands.literal("create").executes(this::createPlayerIslandWorld))
                .then(Commands.literal("delete").executes(this::deletePlayerIslandWorld))
                .then(Commands.literal("tp").executes(this::teleportPlayerToOwnIsland))
                .then(Commands.literal("trust").then(
                                Commands.argument("player", ArgumentTypes.player())
                                        .executes(this::trustPlayerToOwnIsland)
                        )
                )
                .build();
    }

    private int openIslandGUI(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by players");
            return Command.SINGLE_SUCCESS;
        }

        if (!this.playerIslandRecordExists(player)) {
            player.sendMessage(Component.text("You do not have an island", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        IslandGUI islandGUI = new IslandGUI(this.plugin, player);
        player.openInventory(islandGUI.getInventory());

        return Command.SINGLE_SUCCESS;
    }

    private int createPlayerIslandWorld(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by players");
            return Command.SINGLE_SUCCESS;
        }

        if (this.playerIslandRecordExists(player)) {
            player.sendMessage(Component.text("You already have an island, delete it before creating a new one", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

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

        return Command.SINGLE_SUCCESS;
    }

    private void createIslandDatabaseRecord(Player player) {
        String playerUniqueId = player.getUniqueId()
                .toString();

        try {
            User user = this.usersDao.queryForId(playerUniqueId);
            if (user == null) {
                return;
            }
            Island island = this.islandsDao.queryForId(user.getUserId());
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

            this.islandsDao.create(island);
        } catch (SQLException e) {
            // ignore for now
        }
    }

    private int deletePlayerIslandWorld(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by players");
            return Command.SINGLE_SUCCESS;
        }

        if (!this.playerIslandRecordExists(player)) {
            player.sendMessage(Component.text("Unable to delete your island because it does not exist", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        World islandWorld = this.loadOrCreateIslandWorld(player);

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
            return Command.SINGLE_SUCCESS;
        }

        this.deleteIslandDatabaseRecord(player);
        player.sendMessage(Component.text("Successfully deleted your island", NamedTextColor.GREEN));

        return Command.SINGLE_SUCCESS;
    }

    private void deleteIslandDatabaseRecord(Player player) {
        String playerUniqueId = player.getUniqueId()
                .toString();

        try {
            Island island = this.islandsDao.queryForId(playerUniqueId);
            if (island == null) {
                this.plugin.getLogger()
                        .info(String.format("Player %s does not have an island database record to delete",
                                player.getName()));
                return;
            }

            this.islandsDao.delete(island);
        } catch (SQLException e) {
            // ignore for now
        }
    }

    private int teleportPlayerToOwnIsland(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by players");
            return Command.SINGLE_SUCCESS;
        }

        if (!this.playerIslandRecordExists(player)) {
            player.sendMessage(Component.text("You do not have an island to teleport to", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        World islandWorld = this.loadOrCreateIslandWorld(player);

        // TODO: replace with real coordinates set by the owner
        player.teleport(new Location(islandWorld, 0.5, 64, 2.5, 180, 0));

        return Command.SINGLE_SUCCESS;
    }

    private int trustPlayerToOwnIsland(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by players");
            return Command.SINGLE_SUCCESS;
        }

        var resolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
        List<Player> players = resolver.resolve(ctx.getSource());
        if (players.isEmpty()) {
            sender.sendMessage(Component.text("Player not found", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        Player targetPlayer = players.getFirst();
        if (targetPlayer == null) {
            sender.sendMessage(Component.text(String.format("Player %s is not online", targetPlayer.getName()), NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            sender.sendMessage(Component.text("You cannot trust yourself", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        try {
            Island userIsland = islandsDao.queryForId(player.getUniqueId().toString());
            User targetUser = usersDao.queryForId(targetPlayer.getUniqueId().toString());

            if (targetUser == null) {
                sender.sendMessage(Component.text(String.format("Player %s does not exist in our server database", targetPlayer.getName()), NamedTextColor.RED));
                return Command.SINGLE_SUCCESS;
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

        return Command.SINGLE_SUCCESS;
    }

    private boolean playerIslandRecordExists(Player player) {
        try {
            Island playerIsland = this.islandsDao.queryForId(player.getUniqueId().toString());
            return playerIsland != null;
        } catch (SQLException e) {
            // ignore for now
            return false;
        }
    }

    private World loadOrCreateIslandWorld(Player player) {
        String islandName = String.format("islands/%s", player.getUniqueId());

        World islandWorld = Bukkit.getWorld(islandName);
        if (islandWorld == null) {
            islandWorld = WorldCreator.name(islandName).createWorld();
        }

        return islandWorld;
    }
}
