package dev.ricr.skyblock.utils;

import dev.ricr.skyblock.SimpleSkyblock;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IslandManager {
    private record Island(UUID owner, int x, int z) {
    }

    private final SimpleSkyblock plugin;
    private final Map<UUID, Island> islands;
    @Setter
    private boolean opOverride = false;

    public IslandManager(SimpleSkyblock plugin) {
        this.plugin = plugin;
        islands = new HashMap<>();
    }

    public void addPlayerIsland(UUID playerUniqueId) {
        FileConfiguration playerConfig = PlayerUtils.getPlayerConfiguration(plugin, playerUniqueId);
        int x = playerConfig.getInt("x");
        int z = playerConfig.getInt("z");

        islands.put(playerUniqueId, new Island(playerUniqueId, x, z));
    }

    // on disconnect... maybe we dont need this if we want to check if current players have perms?
    public void removePlayerIsland(UUID playerUniqueId) {
        islands.remove(playerUniqueId);
    }

    public void showPlayerBorder(Player player) {
        WorldBorder playerBorder = Bukkit.createWorldBorder();
        playerBorder.setCenter(islands.get(player.getUniqueId())
                .x(), islands.get(player.getUniqueId())
                .z());
        playerBorder.setSize(280);
        player.setWorldBorder(playerBorder);
    }

    public void hidePlayerBorder(Player player) {
        player.setWorldBorder(null);
    }

    public boolean isPlayerInOwnIsland(Player player) {
        int playerPosX = player.getLocation()
                .getBlockX();
        int playerPoxZ = player.getLocation()
                .getBlockZ();
        int islandX = islands.get(player.getUniqueId())
                .x();
        int islandZ = islands.get(player.getUniqueId())
                .z();

        int islandMinX = islandX - ServerUtils.PLAYER_ISLAND_BORDER_RADIUS;
        int islandMaxX = islandX + ServerUtils.PLAYER_ISLAND_BORDER_RADIUS;
        int islandMinZ = islandZ - ServerUtils.PLAYER_ISLAND_BORDER_RADIUS;
        int islandMaxZ = islandZ + ServerUtils.PLAYER_ISLAND_BORDER_RADIUS;

        return playerPosX > islandMinX && playerPoxZ > islandMinZ && playerPosX < islandMaxX && playerPoxZ < islandMaxZ;
    }

    public boolean shouldStopIslandInteraction(Player player) {
        World world = player.getWorld();
        return "void_skyblock".equals(world.getName()) && !this.isPlayerInOwnIsland(player) && !isOpOverride(player);
    }

    private boolean isOpOverride(Player player) {
        return player.isOp() && this.opOverride;
    }
}
