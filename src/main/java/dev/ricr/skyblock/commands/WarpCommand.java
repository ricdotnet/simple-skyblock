package dev.ricr.skyblock.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ricr.skyblock.SimpleSkyblock;
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
                .build();
    }

    private int listPlayerWarps(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        return Command.SINGLE_SUCCESS;
    }

    private int teleportPlayer(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        return Command.SINGLE_SUCCESS;
    }

    private int createWarp(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

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
                    .eq("owner", player.getUniqueId().toString())
                    .query();

            warpEntities.forEach(warp -> builder.suggest(warp.getWarpName()));
        } catch (SQLException e) {
            // ignore for now
        }

        return builder.buildFuture();
    }
}