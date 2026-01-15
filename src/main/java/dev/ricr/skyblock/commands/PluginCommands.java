package dev.ricr.skyblock.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.utils.ServerUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;

import java.util.concurrent.CompletableFuture;

public class PluginCommands {

    public static void register(SimpleSkyblock plugin) {
        new AdminCommand(plugin).register();
        new IslandCommand(plugin).register();
        new GambleCommand(plugin).register();
        new PayCommand(plugin).register();
        new WarpCommand(plugin).register();
        new LobbyCommand(plugin);
        new BalanceCommand(plugin);
        new ShopCommand(plugin);
        new LeaderboardCommand(plugin);
        new AuctionHouseCommand(plugin).register();
    }

    public static CompletableFuture<Suggestions> currentOnlinePlayers(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        var sender = ctx.getSource().getSender();
        ServerUtils.ensureCommandSenderIsPlayer(sender);

        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> builder.suggest(onlinePlayer.getName()));

        return builder.buildFuture();
    }

}
