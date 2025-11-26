package dev.ricr.skyblock.commands;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.shop.BlockItems;
import dev.ricr.skyblock.shop.ShopGUI;
import lombok.AllArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class ShopCommand implements CommandExecutor {
    SimpleSkyblock plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by players!");
            return true;
        }

        ShopGUI shopGUI = new ShopGUI(this.plugin);
        player.openInventory(shopGUI.getInventory());

        return true;
    }
}
