package dev.ricr.skyblock.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;

public interface ICommand {
    void register();
    private LiteralCommandNode<CommandSourceStack> command() {
        return null;
    };
}
