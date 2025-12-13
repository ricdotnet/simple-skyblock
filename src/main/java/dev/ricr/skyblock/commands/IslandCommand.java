package dev.ricr.skyblock.commands;

import com.j256.ormlite.dao.Dao;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.Island;
import dev.ricr.skyblock.database.IslandUserTrustLink;
import dev.ricr.skyblock.database.User;
import dev.ricr.skyblock.gui.IslandGUI;
import dev.ricr.skyblock.utils.NumberUtils;
import dev.ricr.skyblock.utils.PlayerUtils;
import dev.ricr.skyblock.utils.ServerUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.codehaus.plexus.util.FileUtils;

import java.io.IOException;
import java.sql.SQLException;

public class IslandCommand implements ICommand {
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
                            .executes(this::teleportPlayerToOwnIsland)
                            .redirect(island)
                            .build()
                    );
                });
    }

    private LiteralCommandNode<CommandSourceStack> command() {
        return Commands.literal("island")
                .executes(this::teleportPlayerToOwnIsland)
                .then(Commands.literal("menu").executes(this::openIslandGUI))
                .then(Commands.literal("create").executes(this::createPlayerIslandWorld))
                .then(Commands.literal("delete").executes(this::deletePlayerIslandWorld))
                .then(Commands.literal("tp")
                        .executes(this::teleportPlayerToOwnIsland)
                        .then(Commands.literal("set").executes(this::setIslandTeleportPosition))
                )
                .then(Commands.literal("trust").then(
                                Commands.argument("player", ArgumentTypes.player())
                                        .executes(this::trustPlayerToOwnIsland)
                        )
                )
                .then(Commands.literal("visit").then(
                                Commands.argument("player", ArgumentTypes.player())
                                        .executes(this::visitPlayerIsland)
                        )
                )
                .build();
    }

    private int openIslandGUI(CommandContext<CommandSourceStack> ctx) {
        var player = ServerUtils.ensureCommandSenderIsPlayer(ctx.getSource().getSender());

        if (!this.playerIslandRecordExists(player)) {
            player.sendMessage(Component.text("You do not have an island", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        var islandGUI = new IslandGUI(this.plugin, player);
        player.openInventory(islandGUI.getInventory());

        return Command.SINGLE_SUCCESS;
    }

    private int createPlayerIslandWorld(CommandContext<CommandSourceStack> ctx) {
        var player = ServerUtils.ensureCommandSenderIsPlayer(ctx.getSource().getSender());

        var isUsingServerLobby = this.plugin.serverConfig.getBoolean("lobby", true);
        if (!isUsingServerLobby) {
            player.sendMessage(Component.text("Invalid command", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (this.playerIslandRecordExists(player)) {
            player.sendMessage(Component.text("You already have an island, delete it before creating a new one", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        var seed = NumberUtils.newSeed();
        var newIslandWorld = ServerUtils.loadOrCreateWorld(player, World.Environment.NORMAL, seed);

        if (newIslandWorld == null) {
            player.sendMessage(Component.text("Unable to create a new island", NamedTextColor.RED));
        } else {
            var radius = this.plugin.serverConfig.getInt("island.starting_border_radius", 60);

            newIslandWorld.getWorldBorder().setSize(radius * 2 + 1);
            newIslandWorld.getWorldBorder().setCenter(new Location(newIslandWorld, 0.5, 64, 0.5));
            newIslandWorld.save();

            var newLocation = this.plugin.islandGenerator.generateIsland(newIslandWorld, player);
            this.createIslandDatabaseRecord(player, seed, radius);

            newLocation.setY(64);
            newLocation.setX(0.5);
            newLocation.setZ(0.5);
            newLocation.setYaw(180);

            PlayerUtils.saveTpLocation(this.plugin, player, newLocation);

            player.teleport(newLocation);
            player.sendMessage(Component.text("Welcome to your new island", NamedTextColor.GREEN));
        }

        return Command.SINGLE_SUCCESS;
    }

    private void createIslandDatabaseRecord(Player player, long seed, int borderRadius) {
        var playerUniqueId = player.getUniqueId().toString();

        try {
            var user = this.usersDao.queryForId(playerUniqueId);
            if (user == null) {
                return;
            }
            var island = this.islandsDao.queryForId(user.getUserId());
            if (island != null) {
                this.plugin.getLogger()
                        .info(String.format("Player %s already has an island and tried creating another one",
                                player.getName()));
                return;
            }

            island = new Island();
            island.setId(user.getUserId());
            island.setUser(user);
            island.setSeed(seed);
            island.setIslandRadius(borderRadius);

            // Using multiple island worlds means we always start at 0 64 0
            island.setPositionX(0.0d);
            island.setPositionZ(0.0d);

            this.islandsDao.create(island);
        } catch (SQLException e) {
            // ignore for now
        }
    }

    private int deletePlayerIslandWorld(CommandContext<CommandSourceStack> ctx) {
        var player = ServerUtils.ensureCommandSenderIsPlayer(ctx.getSource().getSender());

        var isUsingServerLobby = this.plugin.serverConfig.getBoolean("lobby", true);
        if (!isUsingServerLobby) {
            player.sendMessage(Component.text("Invalid command", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!this.playerIslandRecordExists(player)) {
            player.sendMessage(Component.text("Unable to delete your island because it does not exist", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        var islandWorld = ServerUtils.loadOrCreateWorld(player, null, null);
        var islandNetherWorld = ServerUtils.loadOrCreateWorld(player, World.Environment.NETHER, null);
        var currentWorld = player.getWorld();

        if (!currentWorld.getName().equals("lobby")) {
            var lobbyWorld = Bukkit.getWorld("lobby");
            player.teleport(new Location(lobbyWorld, 0.5, 65, 0.5));
        }

        Bukkit.unloadWorld(islandWorld, false);
        Bukkit.unloadWorld(islandNetherWorld, false);

        var islandWorldFolder = islandWorld.getWorldFolder();
        var islandNetherWorldFolder = islandNetherWorld.getWorldFolder();
        try {
            FileUtils.deleteDirectory(islandWorldFolder);
            FileUtils.deleteDirectory(islandNetherWorldFolder);
        } catch (IOException e) {
            // ignore for now
            player.sendMessage(Component.text("Unable to delete your island", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        this.deleteIslandDatabaseRecord(player);

        player.getInventory().clear();
        player.sendMessage(Component.text("Successfully deleted your island", NamedTextColor.GREEN));

        return Command.SINGLE_SUCCESS;
    }

    private void deleteIslandDatabaseRecord(Player player) {
        var playerUniqueId = player.getUniqueId().toString();

        try {
            var island = this.islandsDao.queryForId(playerUniqueId);
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
        var player = ServerUtils.ensureCommandSenderIsPlayer(ctx.getSource().getSender());

        if (!this.playerIslandRecordExists(player)) {
            player.sendMessage(Component.text("You do not have an island to teleport to", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        var islandLocation = PlayerUtils.getTpLocation(this.plugin, player);
        player.teleport(islandLocation);

        return Command.SINGLE_SUCCESS;
    }

    private int trustPlayerToOwnIsland(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var targetPlayer = this.resolvePlayerFromCommandArgument(sender, ctx);
        if (targetPlayer == null) {
            return Command.SINGLE_SUCCESS;
        }

        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            sender.sendMessage(Component.text("You cannot trust yourself", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        try {
            var userIsland = islandsDao.queryForId(player.getUniqueId().toString());
            var targetUser = usersDao.queryForId(targetPlayer.getUniqueId().toString());

            if (targetUser == null) {
                sender.sendMessage(Component.text(String.format("Player %s does not exist in our server database", targetPlayer.getName()), NamedTextColor.RED));
                return Command.SINGLE_SUCCESS;
            }

            var trustedPlayers = userIsland.getTrustedPlayers();

            var islandUserTrustLink = new IslandUserTrustLink();
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

    private int setIslandTeleportPosition(CommandContext<CommandSourceStack> ctx) {
        var player = ServerUtils.ensureCommandSenderIsPlayer(ctx.getSource().getSender());

        var currentWorld = player.getWorld();
        if (!currentWorld.getName().contains(player.getUniqueId().toString())) {
            player.sendMessage(Component.text("You need to be in your own island to set your teleport position", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        var location = player.getLocation();
        PlayerUtils.saveTpLocation(this.plugin, player, location);

        return Command.SINGLE_SUCCESS;
    }

    private int visitPlayerIsland(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var senderName = player.getName();

        var targetPlayer = this.resolvePlayerFromCommandArgument(sender, ctx);
        if (targetPlayer == null) {
            return Command.SINGLE_SUCCESS;
        }

        var base = Component.text(String.format("%s", senderName), NamedTextColor.GOLD);
        var reason = Component.text("wants to visit your island,", NamedTextColor.GREEN);
        var clickable = Component.text("click here to accept", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.callback(audience -> {
                    var locationToTp = PlayerUtils.getTpLocation(plugin, targetPlayer);
                    player.teleport(locationToTp);
                }));

        try {
            var userIsland = islandsDao.queryForId(player.getUniqueId().toString());
            if (userIsland == null) {
                sender.sendMessage(Component.text(String.format("%s", targetPlayer), NamedTextColor.GOLD)
                        .appendSpace()
                        .append(Component.text("does not have an island", NamedTextColor.RED)));
                return Command.SINGLE_SUCCESS;
            }

            if (userIsland.isPrivate()) {
                sender.sendMessage(Component.text(String.format("%s's", targetPlayer), NamedTextColor.GOLD)
                        .append(Component.text("island is private and you cannot visit", NamedTextColor.RED)));
                return Command.SINGLE_SUCCESS;
            }
        } catch (SQLException e) {
            // ignore for now
        }

        targetPlayer.sendMessage(base.appendSpace().append(reason).appendSpace().append(clickable));

        return Command.SINGLE_SUCCESS;
    }

    private boolean playerIslandRecordExists(Player player) {
        try {
            var playerIsland = this.islandsDao.queryForId(player.getUniqueId().toString());
            return playerIsland != null;
        } catch (SQLException e) {
            // ignore for now
            return false;
        }
    }

    private Player resolvePlayerFromCommandArgument(CommandSender sender, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var resolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
        var players = resolver.resolve(ctx.getSource());
        if (players.isEmpty()) {
            sender.sendMessage(Component.text("Player not found", NamedTextColor.RED));
            return null;
        }

        return players.getFirst();
    }
}
