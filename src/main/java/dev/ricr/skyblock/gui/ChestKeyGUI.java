package dev.ricr.skyblock.gui;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.enums.SoundType;
import dev.ricr.skyblock.items.PlayTimeKey;
import dev.ricr.skyblock.utils.InventoryUtils;
import dev.ricr.skyblock.utils.PlayerUtils;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ChestKeyGUI implements InventoryHolder, ISimpleSkyblockGUI {
    private final SimpleSkyblock plugin;
    @Getter
    private final Inventory inventory;

    public ChestKeyGUI(SimpleSkyblock plugin, Player player, String displayName, List<ItemStack> items) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 27, Component.text(displayName));

        this.loadInventory(items);
        InventoryUtils.fillEmptySlots(this.inventory);

        player.openInventory(this.inventory);
    }

    @Override
    public void handleInventoryClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        var clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        if (!player.getInventory().contains(PlayTimeKey.create(this.plugin))) {
            var noKeyMessage = "<red>You need a PlayTime Key to claim this item";
            player.sendMessage(this.plugin.miniMessage.deserialize(noKeyMessage));
            PlayerUtils.playSound(player, SoundType.NEGATIVE);
            return;
        }

        player.getInventory().remove(PlayTimeKey.create(this.plugin));
        player.give(clicked.clone());

        var successMessage = "<green>Your key has been exchanged for <item>";
        player.sendMessage(this.plugin.miniMessage.deserialize(
                successMessage,
                Placeholder.unparsed("item", ServerUtils.getTextFromComponent(clicked.getItemMeta().displayName()))
        ));
        PlayerUtils.playSound(player, SoundType.POSITIVE);

        this.inventory.close();
    }

    private void loadInventory(List<ItemStack> items) {
        for (int i = 0; i < items.size(); i++) {
            this.inventory.setItem(12 + i, items.get(i));
        }
    }
}
