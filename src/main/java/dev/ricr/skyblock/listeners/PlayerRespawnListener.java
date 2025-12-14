package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.utils.PlayerUtils;
import dev.ricr.skyblock.utils.ServerUtils;
import org.bukkit.Location;
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

        if (!event.isBedSpawn()) {
            this.plugin.getLogger()
                    .info(String.format("Player %s does not have a bed, sending to lobby", player.getName()));

            // TODO: if player has no island teleport to lobby then
            var lobbyWorld = ServerUtils.loadOrCreateLobby();
            event.setRespawnLocation(new Location(lobbyWorld, 0.5, 65, 0.5));
        }

    }
}

