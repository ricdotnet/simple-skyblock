package dev.ricr.skyblock;

import dev.ricr.skyblock.database.PlayerEntity;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OnlinePlayers {
    private final SimpleSkyblock plugin;
    @Getter
    private final Map<UUID, PlayerEntity> onlinePlayers;
    @Getter
    private final Map<UUID, PlayerFastBoard> fastBoards;

    public OnlinePlayers(SimpleSkyblock plugin) {
        this.plugin = plugin;

        this.onlinePlayers = new ConcurrentHashMap<>();
        this.fastBoards = new ConcurrentHashMap<>();

        this.fastBoardUpdater();
    }

    public void addPlayer(UUID uuid, PlayerEntity playerEntity) {
        this.onlinePlayers.put(uuid, playerEntity);

        var player = this.plugin.getServer().getPlayer(uuid);
        if (player == null) {
            this.plugin.getLogger().warning("Player " + uuid + " is not online");
            return;
        }

        this.fastBoards.put(uuid, new PlayerFastBoard(this.plugin, player));
    }

    public void removePlayer(UUID uuid) {
        this.onlinePlayers.remove(uuid);
        this.fastBoards.remove(uuid);
    }

    public PlayerEntity getPlayer(UUID uuid) {
        return this.onlinePlayers.get(uuid);
    }

    private void fastBoardUpdater() {
        this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
            for (var key : this.fastBoards.keySet()) {
                var playerFastBoard = this.fastBoards.get(key);
                var player = this.plugin.getServer().getPlayer(key);
                if (player == null) {
                    continue;
                }
                playerFastBoard.updateMoney(player);
            }
        }, 0, 20);
    }

}
