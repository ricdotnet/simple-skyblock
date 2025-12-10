package dev.ricr.skyblock.generators;

import dev.ricr.skyblock.enums.CustomStructures;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.utils.StructureUtils;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class IslandGenerator {

    @Getter
    private final SimpleSkyblock plugin;

    private final File dataFolder;

    public IslandGenerator(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
    }

    public boolean hasIsland(Player player) {
        File playerFile = new File(dataFolder, player.getUniqueId() + ".yml");
        return playerFile.exists();
    }

    public Location getIslandLocation(Player player) {
        File playerFile = new File(dataFolder, player.getUniqueId() + ".yml");
        if (!playerFile.exists()) {
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        World world = Bukkit.getWorld(config.getString("world", "world"));
        if (world == null) {
            return null;
        }

        return new Location(
            world,
            config.getDouble("x"),
            config.getDouble("y"),
            config.getDouble("z")
        );
    }

    public Location generateIsland(World world, Player player) {
//        World world = player.getWorld();

//        int nextIslandX = this.plugin.serverConfig.getInt("next_island.x");
//        int nextIslandZ = this.plugin.serverConfig.getInt("next_island.z");
//
//        int islandX = nextIslandX;
//        int islandZ = nextIslandZ;
//        int islandY = 64; // Standard sea level
//
//        if (nextIslandX == nextIslandZ && nextIslandX >= 0) {
//            nextIslandX = -nextIslandX - ServerUtils.ISLAND_SPACING;
//        } else if (nextIslandX == -nextIslandZ) {
//            nextIslandX = -nextIslandX;
//        } else {
//            nextIslandZ = -nextIslandX;
//        }

        Location islandLocation = new Location(world, -1.5, 57, -1.5);
        StructureUtils.placeStructure(this.plugin, islandLocation, CustomStructures.ISLAND);
        saveIslandLocation(player, islandLocation);

//        this.plugin.serverConfig.set("next_island.x", nextIslandX);
//        this.plugin.serverConfig.set("next_island.z", nextIslandZ);
//
//        ServerUtils.saveConfig(this.plugin.serverConfig, this.plugin.getDataFolder());
        
        return islandLocation;
    }

    private void saveIslandLocation(Player player, Location location) {
        File playerFile = new File(dataFolder, player.getUniqueId() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        
        config.set("world", location.getWorld().getName());
        config.set("x", location.getX());
        config.set("y", location.getY());
        config.set("z", location.getZ());
        
        try {
            config.save(playerFile);
        } catch (IOException e) {
            this.plugin.getLogger().severe("Failed to save island location for " + player.getName() + ": " + e.getMessage());
        }
    }
}

