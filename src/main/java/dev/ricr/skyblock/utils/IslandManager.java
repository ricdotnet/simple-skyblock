package dev.ricr.skyblock.utils;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.IslandEntity;
import dev.ricr.skyblock.database.IslandPlayerTrustLinkEntity;
import dev.ricr.skyblock.database.PlayerEntity;
import lombok.Getter;
import lombok.Setter;
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
    @Setter
    @Getter
    private boolean opOverride = false;

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
            IslandEntity playerIsland = islandsDao.queryForId(playerUniqueId.toString());

            if (playerIsland == null) {
                return;
            }

            int islandX = (int) playerIsland.getPositionX();
            int islandZ = (int) playerIsland.getPositionZ();
            ForeignCollection<IslandPlayerTrustLinkEntity> trustedPlayers = playerIsland.getTrustedPlayers();

            List<Tuple<String, String>> trustedPlayersId = trustedPlayers.stream().map(
                    trustedPlayer -> new Tuple<>(trustedPlayer.getPlayer()
                            .getPlayerId(), trustedPlayer.getPlayer().getUsername())
            ).collect(ArrayList::new, List::add, List::addAll);

            this.islands.put(playerUniqueId, new IslandRecord(playerUniqueId, islandX, islandZ, trustedPlayersId));
        } catch (SQLException e) {
            // ignore for now
        }
    }

    public boolean isPlayerInOwnIsland(Player player, String worldName) {
        return worldName.contains(player.getUniqueId().toString());
    }

    public boolean shouldStopIslandInteraction(Player player) {
        var world = player.getWorld();

        if (isOpOverride(player) || this.isPlayerInOwnIsland(player, world.getName())) {
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

        return !this.isPlayerInOwnIsland(player, world.getName());
    }

    public boolean shouldStopNetherTeleport(Player player) {
        var world = player.getWorld();

        if (isOpOverride(player) || this.isPlayerInOwnIsland(player, world.getName())) {
            return false;
        }

        if (world.getName().equals("lobby")) {
            return true;
        }

        var islandRecord = this.findCurrentIslandRecord(world.getName());
        if (islandRecord == null) {
            return true;
        }

        for (Tuple<String, String> trustedPlayerTuple : islandRecord.trustedPlayers()) {
            if (player.getUniqueId().toString().equals(trustedPlayerTuple.getFirst())) {
                return false;
            }
        }

        try {
            var island = this.plugin.databaseManager.getIslandsDao().queryForId(player.getUniqueId().toString());
            if (island.isPrivate() || !island.isAllowNetherTeleport()) {
                return true;
            }
        } catch (SQLException e) {
            // ignore for now
        }

        return !this.isPlayerInOwnIsland(player, world.getName());
    }

    private boolean isOpOverride(Player player) {
        return player.isOp() && this.opOverride;
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
