package dev.ricr.skyblock.listeners;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.DatabaseChange;
import dev.ricr.skyblock.database.IslandEntity;
import dev.ricr.skyblock.database.PlayerEntity;
import dev.ricr.skyblock.utils.ServerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.sql.SQLException;

public class PlayerListeners implements Listener {
    private final SimpleSkyblock plugin;

    public PlayerListeners(SimpleSkyblock plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();

        var isUsingLobbyWorld = this.plugin.serverConfig.getBoolean("lobby", true);
        if (!isUsingLobbyWorld) {
            // TODO: implement island creation on join
            return;
        }

        var lobbyWorld = ServerUtils.loadOrCreateLobby();
        player.sendMessage(Component.text("Welcome to SimpleSkyblock!", NamedTextColor.GREEN));

        this.createPlayerEntity(player);
        this.plugin.islandManager.addPlayerIsland(player.getUniqueId());

        // always start in the lobby / spawn world
        player.teleport(new Location(lobbyWorld, 0.5, 65, 0.5));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();

        this.plugin.onlinePlayers.removePlayer(player.getUniqueId());
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

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        var player = event.getPlayer();
        var world = player.getWorld();

        if (world.getName().equals("lobby") || world.getEnvironment() == World.Environment.THE_END) {
            return;
        }

        IslandEntity playerIsland = null;
        var playerUniqueId = player.getUniqueId().toString();
        try {
            playerIsland = this.plugin.databaseManager.getIslandsDao().queryForId(playerUniqueId);
        } catch (SQLException e) {
            // ignore for now
        }

        if (playerIsland == null) {
            return;
        }

        var message = Component.text("-=", NamedTextColor.GREEN)
                .appendSpace()
                .append(Component.text(String.format("%s's island", playerIsland.getPlayer().getUsername()), NamedTextColor.GOLD))
                .appendSpace()
                .append(Component.text("=-", NamedTextColor.GREEN));

        player.sendTitlePart(TitlePart.SUBTITLE, message);
    }

    private void createPlayerEntity(Player player) {
        Dao<PlayerEntity, String> playersDao = this.plugin.databaseManager.getPlayersDao();

        var playerUniqueId = player.getUniqueId();

        try {
            var playerEntity = playersDao.queryForId(playerUniqueId.toString());

            if (playerEntity != null) {
                this.plugin.getLogger()
                        .info(String.format("Player %s already joined before. Skipping initialization of player entity.",
                                player.getName()));

                this.plugin.onlinePlayers.addPlayer(playerUniqueId, playerEntity);
                return;
            }

            playerEntity = new PlayerEntity();
            playerEntity.setPlayerId(playerUniqueId.toString());
            playerEntity.setUsername(player.getName());
            playerEntity.setBalance(100.0d);

            this.plugin.onlinePlayers.addPlayer(playerUniqueId, playerEntity);

            var playerCreateOrUpdate = new DatabaseChange.PlayerCreateOrUpdate(playerEntity);
            this.plugin.databaseChangesAccumulator.add(playerCreateOrUpdate);
        } catch (SQLException e) {
            // ignore for now
        }
    }
}

