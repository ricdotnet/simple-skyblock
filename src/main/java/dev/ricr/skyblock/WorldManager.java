package dev.ricr.skyblock;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldManager {
    private final SimpleSkyblock plugin;
    // this is an active count of players in the world...
    // we keep this count because we cannot rely on world.players().size for when teleport events are happening
    private final Map<String, Integer> activePlayerCount;

    public WorldManager(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.activePlayerCount = new HashMap<>();
    }

    public synchronized World loadOrCreate(UUID playerUniqueId, World.Environment environment, Long seed) {
        var suffix = playerUniqueId.toString() + (environment == World.Environment.NETHER ? "_nether" : "");
        var worldName = String.format("islands/%s", suffix);

        var world = Bukkit.getWorld(worldName);
        if (world == null) {
            var worldCreator = new WorldCreator(worldName);
            if (environment != null) {
                worldCreator.environment(environment);
            }
            if (seed != null) {
                worldCreator.seed(seed);
            }
            worldCreator.generator("SimpleSkyblock");
            world = worldCreator.createWorld();
        }

        this.activePlayerCount.merge(worldName, 1, Integer::sum);

        return world;
    }

    public synchronized World load(String worldName) {
        // TODO: maybe improve?
        var world = Bukkit.getWorld(worldName);
        if (world != null) {
            this.activePlayerCount.merge(worldName, 1, Integer::sum);
        }
        return world;
    }

    public synchronized void unload(World world) {
        var worldName = world.getName();

        var currentActivePlayerCount = this.activePlayerCount.get(worldName);
        if (currentActivePlayerCount == null) {
            return;
        }

        var newActivePlayerCount = currentActivePlayerCount - 1;
        if (newActivePlayerCount <= 0) {
            this.activePlayerCount.remove(worldName);

            Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (world.getPlayers().isEmpty()) {
                    Bukkit.unloadWorld(world, true);
                }
            });
        } else {
            this.activePlayerCount.put(worldName, newActivePlayerCount);
        }
    }

}
