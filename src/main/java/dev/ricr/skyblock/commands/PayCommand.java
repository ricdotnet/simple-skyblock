package dev.ricr.skyblock.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.DatabaseChange;
import dev.ricr.skyblock.utils.ServerUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
public class PayCommand implements ICommand {
    private final SimpleSkyblock plugin;

    public void register() {
        this.plugin.getLifecycleManager()
                .registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
                    LiteralCommandNode<CommandSourceStack> gamble = this.command();

                    commands.registrar().register(gamble);
                });
    }

    private LiteralCommandNode<CommandSourceStack> command() {
        return Commands.literal("pay")
                .then(Commands.argument("player", ArgumentTypes.player())
                        .suggests(this::currentOnlinePlayers)
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(1))
                                .executes(this::pay)
                        )
                )
                .build();
    }

    private int pay(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var sender = ctx.getSource().getSender();

        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);
        var targetPlayer = ServerUtils.resolvePlayerFromCommandArgument(sender, ctx);

        if (targetPlayer == null) {
            player.sendMessage(Component.text("Player not found", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (player.getUniqueId().equals(targetPlayer.getUniqueId())) {
            player.sendMessage(Component.text("You cannot pay yourself", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        var amount = DoubleArgumentType.getDouble(ctx, "amount");

        var targetPlayerRecord = this.plugin.onlinePlayers.getPlayer(targetPlayer.getUniqueId());
        var senderPlayerRecord = this.plugin.onlinePlayers.getPlayer(player.getUniqueId());

        if (senderPlayerRecord.getBalance() >= amount) {
            senderPlayerRecord.setBalance(senderPlayerRecord.getBalance() - amount);
            targetPlayerRecord.setBalance(targetPlayerRecord.getBalance() + amount);
        } else {
            player.sendMessage(Component.text("You don't have enough money to pay that amount",
                    NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        var playerCreateOrUpdateSender = new DatabaseChange.PlayerCreateOrUpdate(senderPlayerRecord);
        var playerCreateOrUpdateTarget = new DatabaseChange.PlayerCreateOrUpdate(targetPlayerRecord);

        this.plugin.databaseChangesAccumulator.add(playerCreateOrUpdateSender);
        this.plugin.databaseChangesAccumulator.add(playerCreateOrUpdateTarget);

        player.sendMessage(Component.text(String.format("Paid %s to %s",
                ServerUtils.formatMoneyValue(amount),
                targetPlayer.getName()), NamedTextColor.GREEN));
        targetPlayer.sendMessage(Component.text(String.format("You received %s from %s",
                        ServerUtils.formatMoneyValue(amount), player.getName()),
                NamedTextColor.GREEN));

        return Command.SINGLE_SUCCESS;
    }

    private CompletableFuture<Suggestions> currentOnlinePlayers(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        var sender = ctx.getSource().getSender();
        ServerUtils.ensureCommandSenderIsPlayer(sender);

        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> builder.suggest(onlinePlayer.getName()));

        return builder.buildFuture();
    }
}
