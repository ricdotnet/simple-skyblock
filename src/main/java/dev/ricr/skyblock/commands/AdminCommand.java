package dev.ricr.skyblock.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.DatabaseChange;
import dev.ricr.skyblock.database.WarpEntity;
import dev.ricr.skyblock.enums.InvalidWarpNames;
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

import java.sql.SQLException;

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
                .then(Commands.literal("createWarp")
                        .then(Commands.argument("warp", StringArgumentType.string())
                                .executes(this::createWarp)
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

    private int createWarp(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var warpName = ctx.getArgument("warp", String.class).toLowerCase();
        var warpEnum = InvalidWarpNames.getByName(warpName);

        if (warpEnum != null && !warpEnum.isAdminOverride()) {
            var message = String.format("<red>This warp name <gold>%s</gold> is marked as invalid and as non-overrideable", warpName);
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
        warpEntity.setServer(true);

        var warpEntityCreateOrUpdate = new DatabaseChange.WarpEntityCreateOrUpdate(warpEntity);
        this.plugin.databaseChangesAccumulator.add(warpEntityCreateOrUpdate);

        var message = String.format("<green>New warp created <gold>%s", warpName);
        sender.sendMessage(this.plugin.miniMessage.deserialize(message));

        return Command.SINGLE_SUCCESS;
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
