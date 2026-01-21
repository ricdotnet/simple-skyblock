package dev.ricr.skyblock.gui;

import dev.ricr.skyblock.DisplayNames;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.items.CreeperCoin;
import dev.ricr.skyblock.items.LuckyPickaxe;
import dev.ricr.skyblock.utils.InventoryUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class PlayTimeChestGUI implements InventoryHolder, ISimpleSkyblockGUI {
    private final SimpleSkyblock plugin;
    @Getter
    private final Inventory inventory;

    public PlayTimeChestGUI(SimpleSkyblock plugin, Player player) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 27, Component.text(DisplayNames.PLAYTIME_KEY_CHEST));

        this.loadInventory();
        InventoryUtils.fillEmptySlots(this.inventory);

        player.openInventory(this.inventory);
    }

    @Override
    public void handleInventoryClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
    }

    private void loadInventory() {
        var creeperCoin = CreeperCoin.create(this.plugin);
        creeperCoin.setAmount(3);

        var luckyPickaxe = LuckyPickaxe.create(this.plugin);

        this.inventory.setItem(10, creeperCoin);
        this.inventory.setItem(11, luckyPickaxe);
    }
}
