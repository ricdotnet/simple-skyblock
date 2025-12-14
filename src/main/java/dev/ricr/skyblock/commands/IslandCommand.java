package dev.ricr.skyblock.commands;

import com.j256.ormlite.dao.Dao;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.DatabaseChange;
import dev.ricr.skyblock.database.IslandEntity;
import dev.ricr.skyblock.database.PlayerEntity;
import dev.ricr.skyblock.gui.IslandGUI;
import dev.ricr.skyblock.utils.NumberUtils;
import dev.ricr.skyblock.utils.PlayerUtils;
import dev.ricr.skyblock.utils.ServerUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.codehaus.plexus.util.FileUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class IslandCommand implements ICommand {
    private final SimpleSkyblock plugin;

    private final Dao<PlayerEntity, String> playersDao;
    private final Dao<IslandEntity, String> islandsDao;

    public IslandCommand(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.playersDao = plugin.databaseManager.getPlayersDao();
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
                .then(Commands.literal("expand").then(
                                Commands.argument("blocks", IntegerArgumentType.integer(1, 10))
                                        .executes(this::expandIsland)
                        )
                )
                .then(Commands.literal("kick").executes(this::kickPlayersFromIsland))
                .then(Commands.literal("trust").then(
                                Commands.argument("player", ArgumentTypes.player())
                                        .executes(this::trustPlayerToOwnIsland)
                        )
                )
                .then(Commands.literal("untrust").then(
                        Commands.argument("player", StringArgumentType.string())
                                .suggests(this::getTrustedPlayersList)
                                .executes(this::untrustPlayerToOwnIsland)
                ))
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
            var playerRecord = this.plugin.onlinePlayers.getPlayer(player.getUniqueId());

            var radius = this.plugin.serverConfig.getInt("island.starting_border_radius", 60);
            var radiusWithOldExpansion = radius + playerRecord.getExpansionSize();

            newIslandWorld.getWorldBorder().setSize(radiusWithOldExpansion * 2 + 1);
            newIslandWorld.getWorldBorder().setCenter(new Location(newIslandWorld, 0.5, 64, 0.5));
            newIslandWorld.save();

            var newLocation = this.plugin.islandGenerator.generateIsland(newIslandWorld, player);
            this.createIslandDatabaseRecord(player, seed);

            newLocation.setY(64);
            newLocation.setX(0.5);
            newLocation.setZ(0.5);
            newLocation.setYaw(180);

            PlayerUtils.saveTpLocation(this.plugin, player, newLocation);
            newIslandWorld.setSpawnLocation(newLocation);

            player.teleport(newLocation);
            player.sendMessage(Component.text("Welcome to your new island", NamedTextColor.GREEN));
        }

        return Command.SINGLE_SUCCESS;
    }

    private void createIslandDatabaseRecord(Player player, long seed) {
        var playerUniqueId = player.getUniqueId().toString();

        try {
            var playerEntity = this.playersDao.queryForId(playerUniqueId);
            if (playerEntity == null) {
                return;
            }
            var island = this.islandsDao.queryForId(playerEntity.getPlayerId());
            if (island != null) {
                this.plugin.getLogger()
                        .info(String.format("Player %s already has an island and tried creating another one",
                                player.getName()));
                return;
            }

            island = new IslandEntity();
            island.setId(playerEntity.getPlayerId());
            island.setPlayer(playerEntity);
            island.setSeed(seed);

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

        var currentPlayersInIslandWorld = islandWorld.getPlayers()
                .stream()
                .filter(p -> !p.getUniqueId().equals(player.getUniqueId()))
                .toList();
        var currentPlayersInIslandNetherWorld = islandNetherWorld.getPlayers()
                .stream()
                .filter(p -> !p.getUniqueId().equals(player.getUniqueId()))
                .toList();

        if (!currentPlayersInIslandWorld.isEmpty() || !currentPlayersInIslandNetherWorld.isEmpty()) {
            player.sendMessage(Component.text("Unable to delete your island because there are players in it", NamedTextColor.RED)
                    .appendNewline()
                    .append(Component.text("Kick everyone out and try again", NamedTextColor.RED)));
            return Command.SINGLE_SUCCESS;
        }

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

        // TODO: check if this would have the same behaviour as using world.getSpawnLocation()
        var islandLocation = PlayerUtils.getTpLocation(this.plugin, player);
        player.teleport(islandLocation);

        return Command.SINGLE_SUCCESS;
    }

    private int trustPlayerToOwnIsland(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var targetPlayer = ServerUtils.resolvePlayerFromCommandArgument(sender, ctx);
        if (targetPlayer == null) {
            return Command.SINGLE_SUCCESS;
        }

        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            sender.sendMessage(Component.text("You cannot trust yourself", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        try {
            var playerIsland = islandsDao.queryForId(player.getUniqueId().toString());
            var targetPlayerEntity = this.playersDao.queryForId(targetPlayer.getUniqueId().toString());

            if (targetPlayerEntity == null) {
                sender.sendMessage(Component.text(String.format("Player %s does not exist in our server database", targetPlayer.getName()), NamedTextColor.RED));
                return Command.SINGLE_SUCCESS;
            }

            var trustedPlayerAdd = new DatabaseChange.TrustedPlayerAdd(playerIsland, targetPlayerEntity);
            this.plugin.databaseChangesAccumulator.add(trustedPlayerAdd);

            var newIslandRecord = this.plugin.islandManager
                    .getIslandRecord(player.getUniqueId())
                    .addTrustedPlayer(targetPlayer.getUniqueId().toString(), targetPlayer.getName());
            this.plugin.islandManager.replaceIslandRecord(player.getUniqueId(), newIslandRecord);

            player.sendMessage(Component.text(String.format("Player %s is now trusted", targetPlayer.getName()), NamedTextColor.GREEN));
        } catch (SQLException e) {
            // ignore for now
        }

        return Command.SINGLE_SUCCESS;
    }

    private int untrustPlayerToOwnIsland(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var targetPlayerName = ctx.getArgument("player", String.class);
        var islandRecord = this.plugin.islandManager.getIslandRecord(player.getUniqueId());
        String targetPlayerUniqueId = null;

        for (var trustedPlayerTuple : islandRecord.trustedPlayers()) {
            if (trustedPlayerTuple.getSecond().equals(targetPlayerName)) {
                targetPlayerUniqueId = trustedPlayerTuple.getFirst();
                break;
            }
        }

        if (targetPlayerUniqueId == null) {
            sender.sendMessage(Component.text(String.format("Player %s is not trusted in your island", targetPlayerName), NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        var trustedPlayerRemove = new DatabaseChange.TrustedPlayerRemove(player.getUniqueId().toString(), targetPlayerUniqueId);
        this.plugin.databaseChangesAccumulator.add(trustedPlayerRemove);

        var newIslandRecord = islandRecord.removeTrustedPlayer(targetPlayerName);
        this.plugin.islandManager.replaceIslandRecord(player.getUniqueId(), newIslandRecord);

        sender.sendMessage(Component.text(String.format("Player %s is no longer trusted in your island", targetPlayerName), NamedTextColor.GREEN));

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

        // set the new tp location for when the player uses /is or /island
        currentWorld.setSpawnLocation(location);

        return Command.SINGLE_SUCCESS;
    }

    private int kickPlayersFromIsland(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var islandWorld = ServerUtils.loadOrCreateWorld(player, null, null);
        var currentPlayersInIsland = islandWorld.getPlayers();
        var lobbyWorld = ServerUtils.loadOrCreateLobby();

        currentPlayersInIsland.forEach(playerInIsland -> {
            if (playerInIsland.getUniqueId().equals(player.getUniqueId())) {
                return;
            }
            playerInIsland.teleport(new Location(lobbyWorld, 0.5, 65, 0.5));
        });

        return Command.SINGLE_SUCCESS;
    }

    private int visitPlayerIsland(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var senderName = player.getName();

        var targetPlayer = ServerUtils.resolvePlayerFromCommandArgument(sender, ctx);
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
            var playerIsland = islandsDao.queryForId(targetPlayer.getUniqueId().toString());
            if (playerIsland == null) {
                sender.sendMessage(Component.text(String.format("%s", targetPlayer.getName()), NamedTextColor.GOLD)
                        .appendSpace()
                        .append(Component.text("does not have an island", NamedTextColor.RED)));
                return Command.SINGLE_SUCCESS;
            }

            if (playerIsland.isPrivate()) {
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

    private int expandIsland(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var islandWorld = ServerUtils.loadOrCreateWorld(player, null, null);
        if (islandWorld == null) {
            player.sendMessage(Component.text("You do not have an island to expand", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        var blocksToExpand = ctx.getArgument("blocks", Integer.class);
        var worldBlocksToExpand = blocksToExpand * 2; // * 2 because otherwise we will expand half a block in all directions
        var priceToExpandPerBlock = this.plugin.serverConfig.getInt("island.expand_price", 10000);
        var totalPriceToExpand = blocksToExpand * priceToExpandPerBlock;

        var playerRecord = this.plugin.onlinePlayers.getPlayer(player.getUniqueId());
        if (playerRecord.getBalance() < totalPriceToExpand) {
            player.sendMessage(Component.text(String.format("You do not have enough money to expand your island by %d blocks", blocksToExpand),
                    NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        var worldBorder = islandWorld.getWorldBorder();
        worldBorder.setSize(worldBorder.getSize() + worldBlocksToExpand);
        worldBorder.setCenter(worldBorder.getCenter());
        islandWorld.save();

        playerRecord.setBalance(playerRecord.getBalance() - totalPriceToExpand);
        playerRecord.setExpansionSize(playerRecord.getExpansionSize() + blocksToExpand); // for keeping track, we keep the original number of blocks

        var playerCreateOrUpdate = new DatabaseChange.PlayerCreateOrUpdate(playerRecord);
        this.plugin.databaseChangesAccumulator.add(playerCreateOrUpdate);

        player.sendMessage(Component.text(String.format("Your island has been expanded by %d blocks", blocksToExpand), NamedTextColor.GREEN));

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

    private CompletableFuture<Suggestions> getTrustedPlayersList(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var islandRecord = this.plugin.islandManager.getIslandRecord(player.getUniqueId());
        if (islandRecord == null) {
            return builder.buildFuture();
        }

        var remaining = builder.getRemaining().toLowerCase();
        islandRecord.trustedPlayers().stream()
                .filter(trustedPlayerTuple -> trustedPlayerTuple.getSecond().toLowerCase().startsWith(remaining))
                .forEach(trustedPlayerTuple -> builder.suggest(trustedPlayerTuple.getSecond()));

        return builder.buildFuture();
    }
}
