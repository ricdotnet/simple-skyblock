package dev.ricr.skyblock.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ricr.skyblock.DisplayNames;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.DatabaseChange;
import dev.ricr.skyblock.database.WarpEntity;
import dev.ricr.skyblock.enums.InvalidWarpNames;
import dev.ricr.skyblock.items.CreeperCoin;
import dev.ricr.skyblock.items.LuckyPickaxe;
import dev.ricr.skyblock.items.PlayTimeKey;
import dev.ricr.skyblock.shop.ShopItems;
import dev.ricr.skyblock.utils.ServerUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.sql.SQLException;

@RequiredArgsConstructor
public class AdminCommand implements ICommand {
    private final SimpleSkyblock plugin;
    private @Nullable BukkitTask opOverrideWarningTask;

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
                .then(Commands.literal("reloadVillagerShops").executes(this::reloadVillagerShops))
                .then(Commands.literal("resetVillagerShops").executes(this::resetVillagerShops))
                .then(Commands.literal("giveMoney")
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                        .executes(this::giveMoney)
                                )
                        )
                )
                .then(Commands.literal("giveCreeperCoins")
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                        .executes(this::giveCreeperCoins)
                                )
                        )
                )
                .then(Commands.literal("giveLuckyPickaxe")
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(this::giveLuckyPickaxe)
                        )
                )
                .then(Commands.literal("givePlayTimeKey")
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(this::givePlayTimeKey)
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

        var updated = !ServerUtils.isOpOverride();
        ServerUtils.setOpOverride(updated);

        if (updated) {
            this.opOverrideWarningTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () ->
                    this.plugin.getServer().getOnlinePlayers().forEach(onlinePlayer -> {
                        if (onlinePlayer.isOp()) {
                            onlinePlayer.sendActionBar(this.plugin.miniMessage.deserialize(DisplayNames.OP_OVERRIDE));
                        }
                    }), 0L, 40L);
        } else {
            if (this.opOverrideWarningTask != null) {
                this.opOverrideWarningTask.cancel();
            }
        }

        var message = "Op override is now " + (updated ? "<green>enabled" : "<red>disabled");
        player.sendMessage(this.plugin.miniMessage.deserialize(message));

        return Command.SINGLE_SUCCESS;
    }

    private int reloadShop(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        ShopItems.loadShop(this.plugin);
        player.sendMessage(Component.text("Shop reloaded successfully!", NamedTextColor.GREEN));

        return Command.SINGLE_SUCCESS;
    }

    private int reloadVillagerShops(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        this.plugin.villagerShopManager.reloadVillagerShops();
        player.sendMessage(Component.text("Villager shops reloaded successfully!", NamedTextColor.GREEN));

        return Command.SINGLE_SUCCESS;
    }

    private int resetVillagerShops(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        this.plugin.villagerShopManager.resetVillagerShops();
        player.sendMessage(Component.text("Villager shops reset successfully!", NamedTextColor.GREEN));

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

        var targetPlayerEntity = this.plugin.onlinePlayers.getPlayer(onlinePlayer.getUniqueId()).getPlayerEntity();
        targetPlayerEntity.setBalance(targetPlayerEntity.getBalance() + amount);

        var playerCreateOrUpdate = new DatabaseChange.PlayerCreateOrUpdate(targetPlayerEntity);
        this.plugin.databaseChangesAccumulator.add(playerCreateOrUpdate);

        targetPlayer.sendMessage(Component.text(String.format("An admin sent you %s", ServerUtils.formatMoneyValue(amount)),
                NamedTextColor.GREEN));

        return Command.SINGLE_SUCCESS;
    }

    private int giveCreeperCoins(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var targetPlayer = ServerUtils.resolvePlayerFromCommandArgument(sender, ctx);
        if (targetPlayer == null) {
            // Should only suggest online players
            player.sendMessage(Component.text("Invalid player", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        var amount = ctx.getArgument("amount", Integer.class);
        var creeperCoin = CreeperCoin.create(this.plugin, amount);
        targetPlayer.give(creeperCoin);

        return Command.SINGLE_SUCCESS;
    }

    private int giveLuckyPickaxe(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var targetPlayer = ServerUtils.resolvePlayerFromCommandArgument(sender, ctx);
        if (targetPlayer == null) {
            // Should only suggest online players
            player.sendMessage(Component.text("Invalid player", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        var luckyPickaxe = LuckyPickaxe.create(this.plugin);
        targetPlayer.give(luckyPickaxe);

        return Command.SINGLE_SUCCESS;
    }

    private int givePlayTimeKey(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var targetPlayer = ServerUtils.resolvePlayerFromCommandArgument(sender, ctx);
        if (targetPlayer == null) {
            // Should only suggest online players
            player.sendMessage(Component.text("Invalid player", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        var playTimeKey = PlayTimeKey.create(this.plugin);
        targetPlayer.give(playTimeKey);

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
