package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.generators.IslandGenerator;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final IslandGenerator islandGenerator;

    public PlayerJoinListener(IslandGenerator islandGenerator) {
        this.islandGenerator = islandGenerator;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!islandGenerator.hasIsland(player)) {
            Location islandLocation = islandGenerator.generateIsland(player);
            Location playerSpawnLocation = islandLocation.clone();
            playerSpawnLocation.add(2.5, 8, 4.5);
            playerSpawnLocation.setYaw(180);

            player.getServer().getScheduler().runTaskLater(
                islandGenerator.getPlugin(),
                () -> player.teleport(playerSpawnLocation),
                20L
            );

            player.setRespawnLocation(playerSpawnLocation, true);
            player.sendMessage("§aWelcome! Your skyblock island has been generated!");
        } else {
            Location playerLastLocation = player.getLocation();
            player.teleport(playerLastLocation);

            player.sendMessage("§aWelcome back!");
        }
    }
}

