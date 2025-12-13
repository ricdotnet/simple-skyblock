package dev.ricr.skyblock.utils;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.Island;
import dev.ricr.skyblock.database.IslandUserTrustLink;
import dev.ricr.skyblock.database.User;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

            if (userIsland == null) {
                return;
            }

            int islandX = (int) userIsland.getPositionX();
            int islandZ = (int) userIsland.getPositionZ();
            ForeignCollection<IslandUserTrustLink> trustedPlayers = userIsland.getTrustedPlayers();

            Set<String> trustedUserIds = trustedPlayers.stream().map(
                    trustedPlayer -> trustedPlayer.getUser()
                            .getUserId()
            ).collect(Collectors.toSet());

            this.islands.put(playerUniqueId, new IslandRecord(playerUniqueId, islandX, islandZ, trustedUserIds));
        } catch (SQLException e) {
            // ignore for now
        }
    }

    public boolean isPlayerInOwnIsland(Player player, String worldName) {
        return player.getUniqueId().toString().contains(worldName);
    }

    public boolean shouldStopIslandInteraction(Player player) {
        var world = player.getWorld();

        if (world.getName().equals("lobby")) {
            return true;
        }

        var islandRecord = this.findCurrentIslandRecord(world.getName());

        // TODO: check this actually makes sense
        if (islandRecord == null) {
            // would mean the current island or place has no owner so we move on
            return false;
        }

        for (String trustedPlayerUniqueId : islandRecord.trustedPlayers()) {
            if (player.getUniqueId().toString().equals(trustedPlayerUniqueId)) {
                return false;
            }
        }

        return !this.isPlayerInOwnIsland(player, world.getName()) && !isOpOverride(player);
    }

    public boolean shouldStopNetherTeleport(Player player) {
        var world = player.getWorld();

        if (world.getName().equals("lobby")) {
            return true;
        }

        var islandRecord = this.findCurrentIslandRecord(world.getName());
        if (islandRecord == null) {
            return true;
        }

        for (String trustedPlayerUniqueId : islandRecord.trustedPlayers()) {
            if (player.getUniqueId().toString().equals(trustedPlayerUniqueId)) {
                return false;
            }
        }

        try {
            var island = this.plugin.databaseManager.getIslandsDao().queryForId(player.getUniqueId().toString());
            if (island.isPrivate() || !island.isAllowNetherVisit()) {
                return true;
            }
        } catch (SQLException e) {
            // ignore for now
        }

        return !this.isPlayerInOwnIsland(player, world.getName()) && !isOpOverride(player);
    }

    private boolean isOpOverride(Player player) {
        return player.isOp() && this.opOverride;
    }

    private IslandRecord findCurrentIslandRecord(String worldName) {
        for (IslandRecord islandRecord : this.islands.values()) {
            if (islandRecord.owner().toString().contains(worldName)) {
                return islandRecord;
            }
        }

        return null;
    }
}
