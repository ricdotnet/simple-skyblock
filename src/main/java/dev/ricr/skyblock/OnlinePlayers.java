package dev.ricr.skyblock;

import dev.ricr.skyblock.database.User;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OnlinePlayers {
    @Getter
    private final Map<UUID, User> onlinePlayers;

    public OnlinePlayers() {
        this.onlinePlayers = new ConcurrentHashMap<>();
    }

    public void addPlayer(UUID uuid, User user) {
        this.onlinePlayers.put(uuid, user);
    }

    public void removePlayer(UUID uuid) {
        this.onlinePlayers.remove(uuid);
    }

    public User getPlayer(UUID uuid) {
        return this.onlinePlayers.get(uuid);
    }

}
