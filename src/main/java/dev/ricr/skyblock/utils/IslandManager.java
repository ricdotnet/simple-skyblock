package dev.ricr.skyblock.utils;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.DatabaseChange;
import dev.ricr.skyblock.database.IslandBlockedPlayersEntity;
import dev.ricr.skyblock.database.IslandEntity;
import dev.ricr.skyblock.database.IslandPlayerTrustLinkEntity;
import dev.ricr.skyblock.database.PlayerEntity;
import dev.ricr.skyblock.permissions.IslandPermissions;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IslandManager {

    private final SimpleSkyblock plugin;
    private final Map<UUID, IslandRecord> islands;

    public IslandManager(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.islands = new HashMap<>();

        Dao<PlayerEntity, String> playersDao = this.plugin.databaseManager.getPlayersDao();

        try {
            List<PlayerEntity> playerEntitiesList = playersDao.queryForAll();
            for (PlayerEntity playerEntity : playerEntitiesList) {
                this.addPlayerIsland(UUID.fromString(playerEntity.getPlayerId()));
            }
        } catch (SQLException e) {
            // ignore for now
        }
    }

    public IslandRecord getIslandRecord(UUID playerUniqueId) {
        return this.islands.get(playerUniqueId);
    }

    public void replaceIslandRecord(UUID playerUniqueId, IslandRecord islandRecord) {
        this.islands.put(playerUniqueId, islandRecord);
    }

    public void addPlayerIsland(UUID playerUniqueId) {
        Dao<IslandEntity, String> islandsDao = this.plugin.databaseManager.getIslandsDao();

        try {
            var playerIsland = islandsDao.queryForId(playerUniqueId.toString());

            if (playerIsland == null) {
                return;
            }

            if (playerIsland.getPermissions() == null) {
                playerIsland.setPermissions(new IslandPermissions(this.plugin, playerUniqueId).toString());

                var islandRecordUpdate = new DatabaseChange.IslandRecordUpdate(playerIsland);
                this.plugin.databaseChangesAccumulator.add(islandRecordUpdate);
            }

            int islandX = (int) playerIsland.getPositionX();
            int islandZ = (int) playerIsland.getPositionZ();
            ForeignCollection<IslandPlayerTrustLinkEntity> trustedPlayers = playerIsland.getTrustedPlayers();
            ForeignCollection<IslandBlockedPlayersEntity> blockedPlayers = playerIsland.getBlockedPlayers();

            List<Tuple<String, String>> trustedPlayersId = trustedPlayers.stream().map(
                    trustedPlayer -> new Tuple<>(trustedPlayer.getPlayer()
                            .getPlayerId(), trustedPlayer.getPlayer().getUsername())
            ).collect(ArrayList::new, List::add, List::addAll);

            List<Tuple<String, String>> blockedPlayersId = blockedPlayers.stream().map(
                    blockedPlayer -> new Tuple<>(blockedPlayer.getPlayer()
                            .getPlayerId(), blockedPlayer.getPlayer().getUsername())
            ).collect(ArrayList::new, List::add, List::addAll);

            var islandPermissions = new IslandPermissions(this.plugin, playerUniqueId, playerIsland.getPermissions());

            this.islands.put(playerUniqueId,
                    new IslandRecord(
                            playerUniqueId, islandX, islandZ, islandPermissions, trustedPlayersId, blockedPlayersId
                    )
            );
        } catch (SQLException e) {
            // ignore for now
        }
    }

    public void removePlayerIsland(UUID playerUniqueId) {
        this.islands.remove(playerUniqueId);
    }

    public boolean shouldStopIslandInteraction(Player player) {
        var world = player.getWorld();

        if (player.isOp() && ServerUtils.isOpOverride() || PlayerUtils.isPlayerInOwnIsland(player, world.getName())) {
            return false;
        }

        if (world.getName().equals("lobby")) {
            return true;
        }

        var islandRecord = this.findCurrentIslandRecord(world.getName());

        // TODO: check this actually makes sense
        if (islandRecord == null) {
            // would mean the current island or place has no owner, so we move on
            return false;
        }

        for (Tuple<String, String> trustedPlayerTuple : islandRecord.trustedPlayers()) {
            if (player.getUniqueId().toString().equals(trustedPlayerTuple.getFirst())) {
                return false;
            }
        }

        return true;
    }

    private IslandRecord findCurrentIslandRecord(String worldName) {
        for (IslandRecord islandRecord : this.islands.values()) {
            if (worldName.contains(islandRecord.owner().toString())) {
                return islandRecord;
            }
        }

        return null;
    }
}
