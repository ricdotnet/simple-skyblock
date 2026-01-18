package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.enchantments.LuckyEnchantment;
import dev.ricr.skyblock.enchantments.PluginEnchantments;
import dev.ricr.skyblock.enums.EventCancellationReasons;
import dev.ricr.skyblock.enums.SoundType;
import dev.ricr.skyblock.permissions.ActionContext;
import dev.ricr.skyblock.permissions.EventCancellations;
import dev.ricr.skyblock.permissions.Policies;
import dev.ricr.skyblock.utils.PlayerUtils;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class LuckyEnchantmentListener implements Listener {
    private final SimpleSkyblock plugin;

    public LuckyEnchantmentListener(SimpleSkyblock plugin) {
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

        if (event.getBlock().getType() != Material.STONE
                || !tool.containsEnchantment(PluginEnchantments.get(PluginEnchantments.LUCKY_ENCHANTMENT))) {
            return;
        }

        if (Math.random() < 0.02) {
            var randomOre = LuckyEnchantment.getRandom();
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(randomOre));
            PlayerUtils.playSound(player, SoundType.POSITIVE);
        }
    }

}
