package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.enums.EventCancellationReasons;
import dev.ricr.skyblock.permissions.EventCancellations;
import dev.ricr.skyblock.utils.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class FeedbackListeners implements Listener {
    private final SimpleSkyblock plugin;

    public FeedbackListeners(SimpleSkyblock plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.isCancelled()) return;

        var cancelReason = EventCancellations.get(event);
        if (cancelReason == null) return;

        this.sendMessage(event.getPlayer(), cancelReason);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!event.isCancelled()) return;

        var cancelReason = EventCancellations.get(event);
        if (cancelReason == null) return;

        this.sendMessage(event.getPlayer(), cancelReason);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (!event.isCancelled()) return;

        var cancelReason = EventCancellations.get(event);
        if (cancelReason == null) return;

        this.sendMessage(event.getPlayer(), cancelReason);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!event.isCancelled()) return;

        var cancelReason = EventCancellations.get(event);
        if (cancelReason == null) return;

        this.sendMessage((Player) event.getDamager(), cancelReason);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!event.isCancelled()) return;

        var cancelReason = EventCancellations.get(event);
        if (cancelReason == null) return;

        this.sendMessage((Player) event.getPlayer(), cancelReason);
    }

    private void sendMessage(Player player, EventCancellationReasons cancelReason) {
        switch (cancelReason) {
            case NO_PERMISSION -> player.sendMessage(Messages.CANNOT_DO_THAT_HERE.component(this.plugin));
            case PLUGIN_BEHAVIOUR -> { /* we ignore this */ }
        }
    }

}
