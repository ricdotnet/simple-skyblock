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
    private final Map<UUID, PlayerScoreboard> scoreboards;

    public OnlinePlayers(SimpleSkyblock plugin) {
        this.plugin = plugin;

        this.onlinePlayers = new ConcurrentHashMap<>();
        this.scoreboards = new ConcurrentHashMap<>();
    }

    public void addPlayer(UUID uuid, PlayerEntity playerEntity) {
        this.onlinePlayers.put(uuid, playerEntity);

        var player = this.plugin.getServer().getPlayer(uuid);
        if (player == null) {
            this.plugin.getLogger().warning("Player " + uuid + " is not online");
            return;
        }
        this.scoreboards.put(uuid, new PlayerScoreboard(this.plugin, player));
    }

    public void removePlayer(UUID uuid) {
        this.onlinePlayers.remove(uuid);
    }

    public PlayerEntity getPlayer(UUID uuid) {
        return this.onlinePlayers.get(uuid);
    }

}
