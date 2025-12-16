package dev.ricr.skyblock.utils;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;

import java.util.concurrent.CompletableFuture;

public class CommandUtils {
    public static CompletableFuture<Suggestions> currentOnlinePlayers(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        var sender = ctx.getSource().getSender();
        ServerUtils.ensureCommandSenderIsPlayer(sender);

        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> builder.suggest(onlinePlayer.getName()));

        return builder.buildFuture();
    }
}
