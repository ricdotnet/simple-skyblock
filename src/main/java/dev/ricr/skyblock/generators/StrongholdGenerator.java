package dev.ricr.skyblock.generators;

import dev.ricr.skyblock.enums.CustomStructures;
import dev.ricr.skyblock.utils.ServerUtils;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.utils.StructureUtils;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.joml.Vector3d;

import java.util.Random;

public class StrongholdGenerator {

    private final SimpleSkyblock plugin;
    @Getter
    private final Vector3d strongholdLocation;

    public StrongholdGenerator(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.strongholdLocation = this.randomCoordinates();

        saveStrongholdLocation(this.strongholdLocation);
    }

    public boolean isStrongholdPlaced() {
        return this.plugin.serverConfig.contains("stronghold_location.placed");
    }

    public void generateEndPortalFrame(World world) {
        this.plugin.getLogger().info("Generating stronghold structure");

        Location strongholdWorldLocation = new Location(world, this.strongholdLocation.x, this.strongholdLocation.y, this.strongholdLocation.z);
        StructureUtils.placeStructure(plugin, strongholdWorldLocation, CustomStructures.STRONGHOLD);
        saveStrongholdPlaced();

        this.plugin.getLogger().info("Stronghold structure generated");
    }

    private void saveStrongholdLocation(Vector3d location) {
        this.plugin.serverConfig.set("stronghold_location.x", location.x);
        this.plugin.serverConfig.set("stronghold_location.y", location.y);
        this.plugin.serverConfig.set("stronghold_location.z", location.z);

        try {
            ServerUtils.saveConfig(this.plugin.serverConfig, this.plugin.getDataFolder());
        } catch (Exception e) {
            this.plugin.getLogger().severe("Failed to save stronghold location: " + e.getMessage());
        }
    }

    private void saveStrongholdPlaced() {
        this.plugin.serverConfig.set("stronghold_location.placed", true);

        ServerUtils.saveConfig(this.plugin.serverConfig, this.plugin.getDataFolder());
    }

    private Vector3d randomCoordinates() {
        double y = -40;
        double x = this.random();
        double z = this.random();

        return new Vector3d(x, y, z);
    }

    private double random() {
        Random random = new Random();
        if (random.nextBoolean()) {
            return ServerUtils.MIN_STRONGHOLD_LOCATION + random.nextInt(ServerUtils.MAX_STRONGHOLD_LOCATION - 1000);
        } else {
            return 1001 + random.nextInt(ServerUtils.MAX_STRONGHOLD_LOCATION - 1000);
        }
    }
}
