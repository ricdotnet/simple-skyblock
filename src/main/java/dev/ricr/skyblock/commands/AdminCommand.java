package dev.ricr.skyblock.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.DatabaseChange;
import dev.ricr.skyblock.shop.ShopItems;
import dev.ricr.skyblock.utils.ServerUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

@AllArgsConstructor
public class AdminCommand implements ICommand {
    private final SimpleSkyblock plugin;

    public void register() {
        this.plugin.getLifecycleManager()
                .registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
                    LiteralCommandNode<CommandSourceStack> admin = this.command();

                    commands.registrar().register(admin);
                });
    }

    private LiteralCommandNode<CommandSourceStack> command() {
        return Commands.literal("admin")
                .requires(sender -> sender.getSender().isOp())
                .then(Commands.literal("opOverride").executes(this::opOverride))
                .then(Commands.literal("reloadShop").executes(this::reloadShop))
                .then(Commands.literal("giveMoney")
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                        .executes(this::giveMoney)
                                )
                        )
                )
                .build();
    }

    private int opOverride(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var isOpOverride = ServerUtils.isOpOverride();
        ServerUtils.setOpOverride(!isOpOverride);

        player.sendMessage(Component.text(String.format("Op override is now %s", ServerUtils.isOpOverride()), NamedTextColor.GREEN));

        return Command.SINGLE_SUCCESS;
    }

    private int reloadShop(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        ShopItems.loadShop(this.plugin);
        player.sendMessage(Component.text("Shop reloaded successfully!", NamedTextColor.GREEN));

        return Command.SINGLE_SUCCESS;
    }

    private int giveMoney(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var targetPlayer = ServerUtils.resolvePlayerFromCommandArgument(sender, ctx);
        if (targetPlayer == null) {
            // Should only suggest online players
            player.sendMessage(Component.text("Invalid player", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        var amount = ctx.getArgument("amount", Double.class);
        var onlinePlayer = Bukkit.getOfflinePlayer(targetPlayer.getUniqueId());

        var targetPlayerRecord = this.plugin.onlinePlayers.getPlayer(onlinePlayer.getUniqueId());
        targetPlayerRecord.setBalance(targetPlayerRecord.getBalance() + amount);

        var playerCreateOrUpdate = new DatabaseChange.PlayerCreateOrUpdate(targetPlayerRecord);
        this.plugin.databaseChangesAccumulator.add(playerCreateOrUpdate);

        targetPlayer.sendMessage(Component.text(String.format("An admin sent you %s", ServerUtils.formatMoneyValue(amount)),
                NamedTextColor.GREEN));

        return Command.SINGLE_SUCCESS;
    }
}
