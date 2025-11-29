package dev.ricr.skyblock.listeners;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.Balance;
import dev.ricr.skyblock.generators.IslandGenerator;
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
        Player player = event.getPlayer();

        this.addInitialBalance(player);

        if (!islandGenerator.hasIsland(player)) {
            Location islandLocation = islandGenerator.generateIsland(player);
            Location playerSpawnLocation = islandLocation.clone();
            playerSpawnLocation.add(2.5, 8, 4.5);
            playerSpawnLocation.setYaw(180);

            player.getServer().getScheduler().runTaskLater(
                islandGenerator.getPlugin(),
                () -> player.teleport(playerSpawnLocation),
                20L
            );

            player.setRespawnLocation(playerSpawnLocation, true);
            player.sendMessage("§aWelcome! Your skyblock island has been generated!");
        } else {
            Location playerLastLocation = player.getLocation();
            player.teleport(playerLastLocation);

            player.sendMessage("§aWelcome back!");
        }
    }

    private void addInitialBalance(Player player) {
        Dao<Balance, String> balanceDao = this.plugin.databaseManager.getBalancesDao();

        try {
            Balance userBalance = balanceDao.queryForId(player.getUniqueId().toString());

            if (userBalance == null) {
                this.plugin.getLogger().info("Creating balance for player " + player.getName());

                userBalance = new Balance();
                userBalance.setUserId(player.getUniqueId().toString());
                userBalance.setValue(100.0d);
                balanceDao.create(userBalance);
            }
        } catch (SQLException e) {
            // ignore for now
        }
    }
}

