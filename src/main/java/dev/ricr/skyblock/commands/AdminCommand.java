package dev.ricr.skyblock.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.shop.ShopItems;
import dev.ricr.skyblock.utils.ServerUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
                .build();
    }

    private int opOverride(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var isOpeOverride = this.plugin.islandManager.isOpOverride();
        this.plugin.islandManager.setOpOverride(!isOpeOverride);

        player.sendMessage(Component.text(String.format("Op override is now %s", this.plugin.islandManager.isOpOverride()), NamedTextColor.GREEN));

        return Command.SINGLE_SUCCESS;
    }

    private int reloadShop(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        ShopItems.loadShop(this.plugin);
        player.sendMessage(Component.text("Shop reloaded successfully!", NamedTextColor.GREEN));

        return Command.SINGLE_SUCCESS;
    }
}
