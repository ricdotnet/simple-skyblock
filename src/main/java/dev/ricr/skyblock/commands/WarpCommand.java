package dev.ricr.skyblock.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.DatabaseChange;
import dev.ricr.skyblock.database.WarpEntity;
import dev.ricr.skyblock.enums.InvalidWarpNames;
import dev.ricr.skyblock.utils.PlayerUtils;
import dev.ricr.skyblock.utils.ServerUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.AllArgsConstructor;

import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
public class WarpCommand implements ICommand {
    private final SimpleSkyblock plugin;

    public void register() {
        this.plugin.getLifecycleManager()
                .registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
                    LiteralCommandNode<CommandSourceStack> warp = this.command();

                    commands.registrar().register(warp);
                });
    }

    private LiteralCommandNode<CommandSourceStack> command() {
        return Commands.literal("warp")
                .then(Commands.literal("list")
                        .executes(this::listPlayerWarps)
                )
                .then(Commands.argument("warp", StringArgumentType.string())
                        .suggests(this::suggestServerAndOwnedWarps)
                        .executes(this::teleportPlayer)
                )
                .then(Commands.literal("create")
                        .then(Commands.argument("warp", StringArgumentType.string())
                                .executes(this::createWarp)
                        )
                )
                .then(Commands.literal("delete")
                        .then(Commands.argument("warp", StringArgumentType.string())
                                .suggests(this::suggestOwnedWarps)
                                .executes(this::deleteWarp)
                        )
                )
                .build();
    }

    private int listPlayerWarps(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        try {
            var playerWarps = this.plugin.databaseManager.getWarpsDao()
                    .queryBuilder()
                    .where()
                    .eq("player_id", player.getUniqueId().toString())
                    .query();

            if (playerWarps.isEmpty()) {
                var message = "<yellow>You don't have any warps";
                sender.sendMessage(this.plugin.miniMessage.deserialize(message));
                return Command.SINGLE_SUCCESS;
            }

            var warpNames = playerWarps.stream()
                    .map(WarpEntity::getWarpName)
                    .collect(java.util.stream.Collectors.joining(", "));

            var message = String.format("<green>Your warps: <gray>%s", warpNames);
            sender.sendMessage(this.plugin.miniMessage.deserialize(message));
        } catch (SQLException e) {
            // ignore for now
        }

        return Command.SINGLE_SUCCESS;
    }

    private int teleportPlayer(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var warpName = ctx.getArgument("warp", String.class).toLowerCase();

        try {
            var warpEntity = this.plugin.databaseManager.getWarpsDao().queryForId(warpName);
            if (warpEntity == null) {
                var message = String.format("<red>Warp <gold>%s</gold> does not exist", warpName);
                player.sendMessage(this.plugin.miniMessage.deserialize(message));
                return Command.SINGLE_SUCCESS;
            }

            var location = ServerUtils.deserializeLocation(warpEntity.getLocation());
            var targetWorld = this.plugin.worldManager.load(location.getWorld().getName());
            if (targetWorld == null) {
                var message = String.format("<red>Warp <gold>%s</gold> is in an invalid world", warpName);
                player.sendMessage(this.plugin.miniMessage.deserialize(message));
                return Command.SINGLE_SUCCESS;
            }

            location.setWorld(targetWorld);
            player.teleport(location);
            var message = String.format("<green>Welcome to Warp <gold>%s", warpName);
            PlayerUtils.showTitleMessage(this.plugin, player, this.plugin.miniMessage.deserialize(message));
        } catch (SQLException e) {
            // ignore for now
        }

        return Command.SINGLE_SUCCESS;
    }

    private int createWarp(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);
        var playerEntity = this.plugin.onlinePlayers.getPlayer(player.getUniqueId());

        var currentWorld = player.getWorld();
        if (currentWorld.getName().contains("lobby")) {
            var message = "<red>You cannot create warps in the lobby world";
            player.sendMessage(this.plugin.miniMessage.deserialize(message));
            return Command.SINGLE_SUCCESS;
        }

        var playerWarpCount = playerEntity.getPlayerWarps().size();
        var maxPlayerWarps = this.plugin.serverConfig.getInt("max_player_warps", 3);
        if (playerWarpCount >= maxPlayerWarps) {
            var message = String.format("<red>You cannot have more than %s warps", maxPlayerWarps);
            player.sendMessage(this.plugin.miniMessage.deserialize(message));
            return Command.SINGLE_SUCCESS;
        }

        var warpName = ctx.getArgument("warp", String.class).toLowerCase();
        var warpEnum = InvalidWarpNames.getByName(warpName);

        if (warpEnum != null) {
            var message = String.format("<red>Invalid warp name <gold>%s", warpName);
            player.sendMessage(this.plugin.miniMessage.deserialize(message));
            return Command.SINGLE_SUCCESS;
        }

        var warpExists = this.warpNameExists(warpName);
        if (warpExists) {
            var message = String.format("<red>Warp with name <gold>%s</gold> already exists", warpName);
            player.sendMessage(this.plugin.miniMessage.deserialize(message));
            return Command.SINGLE_SUCCESS;
        }

        var location = player.getLocation();
        var serializedLocation = ServerUtils.serializeLocation(location);

        var warpEntity = new WarpEntity();
        warpEntity.setWarpName(warpName);
        warpEntity.setLocation(serializedLocation);
        warpEntity.setPlayer(playerEntity);

        var warpEntityCreateOrUpdate = new DatabaseChange.WarpEntityCreateOrUpdate(warpEntity);
        this.plugin.databaseChangesAccumulator.add(warpEntityCreateOrUpdate);

        var message = String.format("<green>New warp created <gold>%s", warpName);
        sender.sendMessage(this.plugin.miniMessage.deserialize(message));

        return Command.SINGLE_SUCCESS;
    }

    private int deleteWarp(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var warpName = ctx.getArgument("warp", String.class).toLowerCase();
        try {
            var warpEntity = this.plugin.databaseManager.getWarpsDao()
                    .queryBuilder()
                    .where()
                    .eq("warp_name", warpName)
                    .and()
                    .eq("player_id", player.getUniqueId().toString())
                    .query();

            if (warpEntity == null) {
                var message = String.format("<red>Warp <gold>%s</gold> does not exist", warpName);
                sender.sendMessage(this.plugin.miniMessage.deserialize(message));
                return Command.SINGLE_SUCCESS;
            }

            this.plugin.databaseManager.getWarpsDao().delete(warpEntity);
        } catch (SQLException e) {
            // ignore for now
        }

        var message = String.format("<green>Warp <gold>%s</gold> deleted", warpName);
        sender.sendMessage(this.plugin.miniMessage.deserialize(message));
        return Command.SINGLE_SUCCESS;
    }

    private CompletableFuture<Suggestions> suggestServerAndOwnedWarps(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        try {
            var warpEntities = this.plugin.databaseManager.getWarpsDao()
                    .queryBuilder()
                    .where()
                    .eq("is_server", true)
                    .or()
                    .eq("player_id", player.getUniqueId().toString())
                    .query();

            warpEntities.forEach(warp -> builder.suggest(warp.getWarpName()));
        } catch (SQLException e) {
            // ignore for now
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestOwnedWarps(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        try {
            var warpEntities = this.plugin.databaseManager.getWarpsDao()
                    .queryBuilder()
                    .where()
                    .eq("player_id", player.getUniqueId().toString())
                    .query();

            warpEntities.forEach(warp -> builder.suggest(warp.getWarpName()));
        } catch (SQLException e) {
            // ignore for now
        }

        return builder.buildFuture();
    }

    private boolean warpNameExists(String warpName) {
        try {
            var warpEntity = this.plugin.databaseManager.getWarpsDao().queryForId(warpName);
            return warpEntity != null;
        } catch (SQLException e) {
            // ignore for now
        }

        // default to false if we get any error
        return true;
    }
}