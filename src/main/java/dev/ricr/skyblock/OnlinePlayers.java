package dev.ricr.skyblock;

import dev.ricr.skyblock.database.PlayerEntity;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OnlinePlayers {
    @Getter
    private final Map<UUID, PlayerEntity> onlinePlayers;

    public OnlinePlayers() {
        this.onlinePlayers = new ConcurrentHashMap<>();
    }

    public void addPlayer(UUID uuid, PlayerEntity playerEntity) {
        this.onlinePlayers.put(uuid, playerEntity);
    }

    public void removePlayer(UUID uuid) {
        this.onlinePlayers.remove(uuid);
    }

    public PlayerEntity getPlayer(UUID uuid) {
        return this.onlinePlayers.get(uuid);
    }

}
