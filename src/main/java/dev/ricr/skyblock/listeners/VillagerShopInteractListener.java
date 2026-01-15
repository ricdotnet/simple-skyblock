package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.enums.EventCancellationReasons;
import dev.ricr.skyblock.gui.VillagerShopGUI;
import dev.ricr.skyblock.permissions.EventCancellations;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class VillagerShopInteractListener implements Listener {
    private final SimpleSkyblock plugin;

    public VillagerShopInteractListener(SimpleSkyblock plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onVillagerShopInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }

        var villagerShopUniqueId = villager.getUniqueId();
        var villagerShop = this.plugin.villagerShopManager.getVillagerShop(villagerShopUniqueId);

        if (villagerShop == null) {
            return;
        }

        event.setCancelled(true);
        EventCancellations.replace(event, EventCancellationReasons.PLUGIN_BEHAVIOUR);

        new VillagerShopGUI(this.plugin, villagerShop.getName(), event.getPlayer(), villagerShop.getItems(), villagerShop.getColor());
    }

}
