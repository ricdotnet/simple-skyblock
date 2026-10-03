package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.enums.CustomItems;
import dev.ricr.skyblock.enums.EventCancellationReasons;
import dev.ricr.skyblock.enums.SoundType;
import dev.ricr.skyblock.items.LuckyPickaxe;
import dev.ricr.skyblock.permissions.ActionContext;
import dev.ricr.skyblock.permissions.EventCancellations;
import dev.ricr.skyblock.permissions.Policies;
import dev.ricr.skyblock.utils.PlayerUtils;
import dev.ricr.skyblock.utils.ServerUtils;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class LuckyPickaxeListener implements Listener {
    private final SimpleSkyblock plugin;

    public LuckyPickaxeListener(SimpleSkyblock plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        var player = event.getPlayer();
        var tool = event.getPlayer().getInventory().getItemInMainHand();

        var actionContext = new ActionContext(this.plugin, player, event);
        if (!Policies.BREAK_BLOCKS.test(actionContext)) {
            EventCancellations.add(event, EventCancellationReasons.NO_PERMISSION);
            return;
        }

        var itemPersistentDataContainer = tool.getPersistentDataContainer();
        var customItem = itemPersistentDataContainer.get(ServerUtils.CUSTOM_ITEM, PersistentDataType.STRING);
        if (customItem == null) {
            return;
        }
        var customItemType = CustomItems.valueOf(customItem);

        if (event.getBlock().getType() != Material.STONE || customItemType != CustomItems.LUCKY_PICKAXE) {
            return;
        }

        if (Math.random() < 0.10) {
            var randomOre = LuckyPickaxe.getRandom();
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(randomOre));
            PlayerUtils.playSound(player, SoundType.POSITIVE);
        }
    }

}
