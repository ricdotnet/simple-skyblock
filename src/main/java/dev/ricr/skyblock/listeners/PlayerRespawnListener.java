package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.utils.PlayerUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerRespawnListener implements Listener {

    private final SimpleSkyblock plugin;

    public PlayerRespawnListener(SimpleSkyblock plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        this.plugin.getLogger()
                .info(String.format("Respawning %s in X: %s - Y: %s - Z: %s", player.getName(), player.getLocation()
                        .getBlockX(), player.getLocation()
                        .getBlockY(), player.getLocation()
                        .getBlockZ()));

        if (!event.isBedSpawn()) {
            this.plugin.getLogger()
                    .info(String.format("Resetting spawn for %s", player.getName()));

            event.setRespawnLocation(PlayerUtils.getPlayerIslandLocation(this.plugin, event.getPlayer()));
        }
    }
}

