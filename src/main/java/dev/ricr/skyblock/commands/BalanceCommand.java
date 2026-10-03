package dev.ricr.skyblock.commands;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.utils.ServerUtils;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.List;

public class BalanceCommand implements BasicCommand {
    private final SimpleSkyblock plugin;

    public BalanceCommand(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.plugin.registerCommand("balance", List.of("bal"), this);
    }

    @Override
    public void execute(CommandSourceStack commandSourceStack, String[] args) {
        var sender = commandSourceStack.getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);
        var playerEntity = this.plugin.onlinePlayers.getPlayer(player.getUniqueId()).getPlayerEntity();

        var message = String.format("Your balance is <color:#F23CC7A>%s", ServerUtils.formatMoneyValue(playerEntity.getBalance()));
        player.sendMessage(this.plugin.miniMessage.deserialize(message));
    }
}
