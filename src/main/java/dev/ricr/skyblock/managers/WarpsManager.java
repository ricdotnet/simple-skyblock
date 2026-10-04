package dev.ricr.skyblock.managers;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.records.Warp;
import dev.ricr.skyblock.utils.ServerUtils;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarpsManager {
    private final SimpleSkyblock plugin;
    private final Map<String, Warp> warps = new HashMap<>();

    public WarpsManager(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.registerServerWarps();
        this.registerPlayerWarps();
    }

    private void registerServerWarps() {
        var warps = plugin.serverConfig.getMapList("warps.server");

        warps.forEach(warp -> {
            var warpName = (String) warp.get("name");

            this.plugin.getLogger().info("Registering server warp: " + warpName);

            this.addWarp(new Warp(warpName, "server", new Location(ServerUtils.loadOrCreateLobby(), 0, 0, 0)));
        });
    }

    private void registerPlayerWarps() {
        try {
            var warpEntities = this.plugin.databaseManager.getWarpsDao().queryForAll();

            warpEntities.forEach(warp -> {
                var warpName = warp.getWarpName();
                var playerId = warp.getPlayer().getPlayerId();

                var location = ServerUtils.deserializeLocation(this.plugin, warp);

                this.plugin.getLogger().info("Registering player warp: " + warpName + " for player: " + playerId);

                this.addWarp(new Warp(warpName, playerId, location));
            });
        } catch (SQLException e) {
            // ignore for now
        }
    }

    public void addWarp(Warp warp) {
        warps.put(warp.name(), warp);
    }

    public void removeWarp(String name) {
        warps.remove(name);
    }

    public Warp getWarp(String name) {
        return warps.get(name);
    }

    public boolean warpExists(String name) {
        return warps.containsKey(name);
    }

    public List<Warp> getPlayerWarps(String playerId) {
        return warps.values().stream()
                .filter(warp -> warp.owner().equals(playerId))
                .toList();
    }

    public boolean isReservedWarpName(String warpName) {
        var reservedWarpNames = this.plugin.serverConfig.getMapList("warps.reserved_names");

        return reservedWarpNames.stream()
                .anyMatch(name -> name.get("name").equals(warpName));
    }
}
