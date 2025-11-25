package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class MobSpawnListener implements Listener {
    SimpleSkyblock plugin;

    public MobSpawnListener(SimpleSkyblock plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPassiveSpawn(CreatureSpawnEvent event) {
        this.plugin.getLogger().info("Trying to spawn: " + event.getEntityType());

        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;

        EntityType type = event.getEntityType();

        if (type == EntityType.COW || type == EntityType.SHEEP || type == EntityType.PIG) {
            Location spawnLocation = event.getLocation();
            Biome biome = spawnLocation.getBlock().getBiome();

            if (biome == Biome.DEEP_COLD_OCEAN) {
                // We cancel the original spawn event and spawn the entity ourselves
                event.setCancelled(true);
                spawnLocation.getWorld().spawnEntity(spawnLocation, type);
            }
        }
    }
}
