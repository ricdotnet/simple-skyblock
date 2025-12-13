package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

@AllArgsConstructor
public class ServerLoadListener implements Listener {
    private final SimpleSkyblock plugin;

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        ServerUtils.setEndPortalTextDisplay(this.plugin);
    }
}
