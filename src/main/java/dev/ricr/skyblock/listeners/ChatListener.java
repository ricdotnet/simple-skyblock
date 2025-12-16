package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class ChatListener implements Listener, ChatRenderer {

    public ChatListener(SimpleSkyblock plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        event.renderer(this); // Tell the event to use our renderer
    }

    @Override
    @NotNull
    public Component render(@NotNull Player source, @NotNull Component sourceDisplayName, @NotNull Component message, @NotNull Audience viewer) {
        return Component.text()
                .append(Component.text(String.format("%s", source.getName()), NamedTextColor.WHITE))
                .append(Component.text(" » ", NamedTextColor.GRAY))
                .append(message)
                .build();
    }
}
