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
        Location islandLocation = new Location(world, -5, 61, -5);
        StructureUtils.placeStructure(this.plugin, islandLocation, CustomStructures.ISLAND);
        saveIslandLocation(player, islandLocation);
        
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

