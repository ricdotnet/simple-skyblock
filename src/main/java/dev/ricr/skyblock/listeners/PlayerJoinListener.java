package dev.ricr.skyblock.listeners;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.Balance;
import dev.ricr.skyblock.database.User;
import dev.ricr.skyblock.generators.IslandGenerator;
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
    private final IslandGenerator islandGenerator;

    public PlayerJoinListener(SimpleSkyblock plugin, IslandGenerator islandGenerator) {
        this.plugin = plugin;
        this.islandGenerator = islandGenerator;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        var lobbyWorld = Bukkit.getWorld("lobby");

        player.sendMessage(Component.text("Welcome to SimpleSkyblock!", NamedTextColor.GREEN));

//        if (!islandGenerator.hasIsland(player)) {
//            Location islandLocation = islandGenerator.generateIsland(player.getWorld(), player);
//            Location playerSpawnLocation = islandLocation.clone();
//            playerSpawnLocation.add(2.5, 8, 4.5);
//            playerSpawnLocation.setYaw(180);
//
//            player.getServer()
//                    .getScheduler()
//                    .runTaskLater(
//                            islandGenerator.getPlugin(),
//                            () -> player.teleport(playerSpawnLocation),
//                            20L
//                    );
//
//            player.setRespawnLocation(playerSpawnLocation, true);
//            player.sendMessage("§aWelcome! Your skyblock island has been generated!");
//        } else {
//            Location playerLastLocation = player.getLocation();
//            player.teleport(playerLastLocation);
//
//            player.sendMessage("§aWelcome back!");
//        }

        this.initializeUserTable(player);
        this.plugin.islandManager.addPlayerIsland(player.getUniqueId());

        // always start in the lobby / spawn world
        player.teleport(new Location(lobbyWorld, 0.5, 65, 0.5));
    }

    private void initializeUserTable(Player player) {
        Dao<User, String> userDao = this.plugin.databaseManager.getUsersDao();
        // get balance to migrate
        Dao<Balance, String> balanceDao = this.plugin.databaseManager.getBalancesDao();

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

            Balance userBalance = balanceDao.queryForId(playerUniqueId);

            user = new User();
            user.setUserId(player.getUniqueId()
                    .toString());
            user.setUsername(player.getName());

            if (userBalance == null) {
                user.setBalance(100.0d);
            } else {
                user.setBalance(userBalance.getValue());
            }

            userDao.create(user);
        } catch (SQLException e) {
            // ignore for now
        }
    }

    @Deprecated(forRemoval = true)
    private void addInitialBalance(Player player) {
        Dao<Balance, String> balanceDao = this.plugin.databaseManager.getBalancesDao();

        try {
            Balance userBalance = balanceDao.queryForId(player.getUniqueId()
                    .toString());

            if (userBalance == null) {
                this.plugin.getLogger()
                        .info("Creating balance for player " + player.getName());

                userBalance = new Balance();
                userBalance.setUserId(player.getUniqueId()
                        .toString());
                userBalance.setValue(100.0d);
                balanceDao.create(userBalance);
            }
        } catch (SQLException e) {
            // ignore for now
        }
    }
}

