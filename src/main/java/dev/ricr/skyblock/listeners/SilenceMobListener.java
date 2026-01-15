package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.enums.EventCancellationReasons;
import dev.ricr.skyblock.permissions.ActionContext;
import dev.ricr.skyblock.permissions.EventCancellations;
import dev.ricr.skyblock.permissions.Policies;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class SilenceMobListener implements Listener {
    private final SimpleSkyblock plugin;

    public SilenceMobListener(SimpleSkyblock plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onSilenceMob(PlayerInteractEntityEvent event) {
        var player = event.getPlayer();
        var itemInHand = player.getInventory().getItem(event.getHand());
        var interactedEntity = event.getRightClicked();

        var actionContext = new ActionContext(this.plugin, player, event);
        if (!Policies.INTERACT_WITH_MOBS.test(actionContext)) {
            EventCancellations.add(event, EventCancellationReasons.NO_PERMISSION);
            return;
        }

        if (Material.NAME_TAG != itemInHand.getType() && !(interactedEntity instanceof LivingEntity)) {
            return;
        }

        if (!itemInHand.hasItemMeta() || !itemInHand.getItemMeta().hasDisplayName()) {
            return;
        }

        var tagName = itemInHand.getItemMeta().displayName();
        var plain = PlainTextComponentSerializer.plainText().serialize(tagName);

        if (!plain.equalsIgnoreCase("[silence]")) {
            return;
        }

        interactedEntity.setSilent(true);
    }

}
