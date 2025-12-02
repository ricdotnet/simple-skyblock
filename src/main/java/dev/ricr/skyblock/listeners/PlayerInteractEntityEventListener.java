package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class PlayerInteractEntityEventListener implements Listener {
    private final SimpleSkyblock plugin;

    public PlayerInteractEntityEventListener(SimpleSkyblock plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (!this.plugin.islandManager.canInteractInCurrentIsland(player)) {
            player.sendMessage(Component.text("You cannot do that here",
                    NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }
    }
}
