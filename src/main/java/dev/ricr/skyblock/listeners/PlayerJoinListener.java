package dev.ricr.skyblock.listeners;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.DatabaseChange;
import dev.ricr.skyblock.database.PlayerEntity;
import dev.ricr.skyblock.utils.ServerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.SQLException;

public class PlayerJoinListener implements Listener {
    private final SimpleSkyblock plugin;

    public PlayerJoinListener(SimpleSkyblock plugin) {
        this.plugin = plugin;
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

