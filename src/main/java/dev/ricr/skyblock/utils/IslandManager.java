package dev.ricr.skyblock.utils;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.Island;
import dev.ricr.skyblock.database.User;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class IslandManager {

    private final SimpleSkyblock plugin;
    private final Map<UUID, IslandRecord> islands;
    @Setter
    private boolean opOverride = false;

    public IslandManager(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.islands = new HashMap<>();

        Dao<User, String> usersDao = this.plugin.databaseManager.getUsersDao();

        try {
            List<User> userList = usersDao.queryForAll();
            for (User user : userList) {
                this.addPlayerIsland(UUID.fromString(user.getUserId()));
            }
        } catch (SQLException e) {
            // ignore for now
        }
    }

    public IslandRecord getIslandRecord(UUID playerUniqueId) {
        return this.islands.get(playerUniqueId);
    }

    public void addPlayerIsland(UUID playerUniqueId) {
        Dao<Island, String> islandsDao = this.plugin.databaseManager.getIslandsDao();

        try {
            Island userIsland = islandsDao.queryForId(playerUniqueId.toString());

            int islandX = (int) userIsland.getPositionX();
            int islandZ = (int) userIsland.getPositionZ();
            Set<UUID> trustedPlayers = userIsland.getTrustedPlayers();

            this.islands.put(playerUniqueId, new IslandRecord(playerUniqueId, islandX, islandZ, trustedPlayers));
        } catch (SQLException e) {
            // ignore for now
        }
    }

    public void showPlayerBorder(Player player) {
        WorldBorder playerBorder = Bukkit.createWorldBorder();
        playerBorder.setCenter(this.islands.get(player.getUniqueId())
                .x(), this.islands.get(player.getUniqueId())
                .z());
        playerBorder.setSize(280);
        player.setWorldBorder(playerBorder);
    }

    public void hidePlayerBorder(Player player) {
        player.setWorldBorder(null);
    }

    public boolean isPlayerInOwnIsland(Player player) {
        int playerPosX = player.getLocation()
                .getBlockX();
        int playerPoxZ = player.getLocation()
                .getBlockZ();
        int islandX = this.islands.get(player.getUniqueId())
                .x();
        int islandZ = this.islands.get(player.getUniqueId())
                .z();

        int islandMinX = islandX - ServerUtils.PLAYER_ISLAND_BORDER_RADIUS;
        int islandMaxX = islandX + ServerUtils.PLAYER_ISLAND_BORDER_RADIUS;
        int islandMinZ = islandZ - ServerUtils.PLAYER_ISLAND_BORDER_RADIUS;
        int islandMaxZ = islandZ + ServerUtils.PLAYER_ISLAND_BORDER_RADIUS;

        return playerPosX > islandMinX && playerPoxZ > islandMinZ && playerPosX < islandMaxX && playerPoxZ < islandMaxZ;
    }

    public boolean shouldStopIslandInteraction(Player player) {
        World world = player.getWorld();
        boolean isTrustedPlayer = false;

        IslandRecord islandRecord = findCurrentIslandByPlayerPosition(player);

        // TODO: check this actually makes sense
        if (islandRecord == null) {
            // would mean the current island or place has no owner so we move on
            return false;
        }

        for (UUID trustedPlayerUniqueId : islandRecord.trustedPlayers()) {
            if (player.getUniqueId().equals(trustedPlayerUniqueId)) {
                isTrustedPlayer = true;
            }
        }

        if (isTrustedPlayer) {
            return false;
        }

        return "void_skyblock".equals(world.getName()) && !this.isPlayerInOwnIsland(player) && !isOpOverride(player);
    }

    private boolean isOpOverride(Player player) {
        return player.isOp() && this.opOverride;
    }

    private IslandRecord findCurrentIslandByPlayerPosition(Player player) {
        int playerPosX = player.getLocation().getBlockX();
        int playerPosZ = player.getLocation().getBlockZ();

        for (IslandRecord islandRecord : this.islands.values()) {
            int islandMinX = islandRecord.x() - ServerUtils.PLAYER_ISLAND_BORDER_RADIUS;
            int islandMaxX = islandRecord.x() + ServerUtils.PLAYER_ISLAND_BORDER_RADIUS;
            int islandMinZ = islandRecord.z() - ServerUtils.PLAYER_ISLAND_BORDER_RADIUS;
            int islandMaxZ = islandRecord.z() + ServerUtils.PLAYER_ISLAND_BORDER_RADIUS;

            if (playerPosX >= islandMinX && playerPosX <= islandMaxX && playerPosZ >= islandMinZ && playerPosZ <= islandMaxZ) {
                return islandRecord;
            }
        }

        return null;
    }
}
