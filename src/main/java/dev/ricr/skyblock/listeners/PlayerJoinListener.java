package dev.ricr.skyblock.listeners;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.User;
import dev.ricr.skyblock.generators.IslandGenerator;
import dev.ricr.skyblock.utils.ServerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

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

        this.initializeUserTable(player);
        this.plugin.islandManager.addPlayerIsland(player.getUniqueId());

        // always start in the lobby / spawn world
        player.teleport(new Location(lobbyWorld, 0.5, 65, 0.5));
    }

    private void initializeUserTable(Player player) {
        Dao<User, String> userDao = this.plugin.databaseManager.getUsersDao();

        String playerUniqueId = player.getUniqueId()
                .toString();

        try {
            User user = userDao.queryForId(playerUniqueId);

            if (user != null) {
                this.plugin.getLogger()
                        .info(String.format("Player %s already joined before. Skipping initialization of user record.",
                                player.getName()));
                return;
            }

            user = new User();
            user.setUserId(player.getUniqueId()
                    .toString());
            user.setUsername(player.getName());
            user.setBalance(100.0d);

            userDao.create(user);
        } catch (SQLException e) {
            // ignore for now
        }
    }
}

