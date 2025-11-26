package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.generators.StrongholdGenerator;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.joml.Vector3d;

public class ChunkLoadListener implements Listener {

    private final SimpleSkyblock plugin;
    private final StrongholdGenerator strongholdGenerator;

    public ChunkLoadListener(SimpleSkyblock plugin, StrongholdGenerator strongholdGenerator) {
        this.plugin = plugin;
        this.strongholdGenerator = strongholdGenerator;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        World world = event.getWorld();
        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();

        // Check if a stronghold has not been placed yet and place it if it hasn't
        if (!this.strongholdGenerator.isStrongholdPlaced()) {
            Vector3d strongholdLocation = this.strongholdGenerator.getStrongholdLocation();
            int strongholdChunkX = (int) strongholdLocation.x >> 4;
            int strongholdChunkZ = (int) strongholdLocation.z >> 4;

            if ((chunkX == strongholdChunkX && chunkZ == strongholdChunkZ)) {
                this.plugin.getLogger().info("Placing stronghold");

                this.strongholdGenerator.generateEndPortalFrame(world);
            }
        }
    }
}