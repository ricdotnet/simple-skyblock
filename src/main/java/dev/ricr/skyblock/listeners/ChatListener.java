package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.utils.ServerUtils;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

public class ChatListener implements Listener, ChatRenderer {
    private final SimpleSkyblock plugin;

    public ChatListener(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        event.renderer(this); // Tell the event to use our renderer
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();

        if (ServerUtils.isOpOverride() && player.isOp()) {
            return;
        }

        if (message.equalsIgnoreCase("/seed") || message.contains("/locate")) {
            event.setCancelled(true);
            player.sendMessage(this.plugin.miniMessage.deserialize("<red>You are not allowed to use this command"));
        }
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
