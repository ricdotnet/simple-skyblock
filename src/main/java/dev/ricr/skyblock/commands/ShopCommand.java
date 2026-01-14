package dev.ricr.skyblock.commands;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.gui.ShopTypeGUI;
import dev.ricr.skyblock.utils.ServerUtils;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

public class ShopCommand implements BasicCommand {
    private final SimpleSkyblock plugin;

    public ShopCommand(SimpleSkyblock plugin) {
        this.plugin = plugin;

        this.plugin.registerCommand("shop", this);
    }

    @Override
    public void execute(CommandSourceStack commandSourceStack, String[] args) {
        var sender = commandSourceStack.getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        ShopTypeGUI shopTypeGUI = new ShopTypeGUI(this.plugin);
        player.openInventory(shopTypeGUI.getInventory());
    }
}
