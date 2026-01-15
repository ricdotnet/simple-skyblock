package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.enchantments.LuckyEnchantment;
import dev.ricr.skyblock.enchantments.PluginEnchantments;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class LuckyEnchantmentListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        var player = event.getPlayer();
        var tool = event.getPlayer().getInventory().getItemInMainHand();

        if (event.getBlock().getType() != Material.STONE || !tool.containsEnchantment(PluginEnchantments.get(PluginEnchantments.LUCKY_ENCHANTMENT))) {
            return;
        }

        if (Math.random() < 0.02) {
            var randomOre = LuckyEnchantment.getRandom();
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(randomOre));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        }
    }

}
