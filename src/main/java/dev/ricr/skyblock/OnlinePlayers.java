package dev.ricr.skyblock;

import dev.ricr.skyblock.database.PlayerEntity;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OnlinePlayers {
    private final SimpleSkyblock plugin;
    @Getter
    private final Map<UUID, OnlinePlayer> onlinePlayers;

    public OnlinePlayers(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.onlinePlayers = new ConcurrentHashMap<>();
        this.fastBoardUpdater();
    }

    public void addPlayer(Player player, PlayerEntity playerEntity) {
        var playerUUID = player.getUniqueId();

        this.onlinePlayers.put(playerUUID, new OnlinePlayer(this.plugin, player, playerEntity));
    }

    public void removePlayer(UUID uuid) {
        this.onlinePlayers.remove(uuid);
    }

    public OnlinePlayer getPlayer(UUID uuid) {
        return this.onlinePlayers.get(uuid);
    }

    private void fastBoardUpdater() {
        this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
            for (var onlinePlayer : this.onlinePlayers.values()) {
                var playerFastBoard = onlinePlayer.getFastBoard();
                playerFastBoard.updateMoney();
                playerFastBoard.updateWorldTime();
                playerFastBoard.updatePing();
            }
        }, 0, 20);
    }

    public static class OnlinePlayer {
        private final SimpleSkyblock plugin;
        @Getter
        private final Player player;
        @Getter
        private final PlayerEntity playerEntity;
        @Getter
        private final PlayerFastBoard fastBoard;

        public OnlinePlayer(SimpleSkyblock plugin, Player player, PlayerEntity playerEntity) {
            this.plugin = plugin;
            this.player = player;
            this.playerEntity = playerEntity;
            this.fastBoard = new PlayerFastBoard(this.plugin, player, playerEntity);
        }
    }

}
