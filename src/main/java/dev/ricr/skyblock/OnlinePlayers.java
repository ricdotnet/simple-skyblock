package dev.ricr.skyblock;

import dev.ricr.skyblock.database.PlayerEntity;
import dev.ricr.skyblock.enums.SoundType;
import dev.ricr.skyblock.items.PlayTimeKey;
import dev.ricr.skyblock.utils.PlayerUtils;
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

    public void removePlayer(UUID playerUniqueId) {
        var onlinePlayer = this.onlinePlayers.remove(playerUniqueId);

        var playerConfig = PlayerUtils.getPlayerConfiguration(this.plugin, playerUniqueId);
        playerConfig.set("play_time_key_countdown", onlinePlayer.playTimeKeyRemaining + 1);
        PlayerUtils.savePlayerConfiguration(this.plugin, playerConfig, playerUniqueId);
    }

    public OnlinePlayer getPlayer(UUID playerUniqueId) {
        return this.onlinePlayers.get(playerUniqueId);
    }

    private void fastBoardUpdater() {
        this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
            for (var onlinePlayer : this.onlinePlayers.values()) {
                var playerFastBoard = onlinePlayer.getFastBoard();
                playerFastBoard.updateMoney();
                playerFastBoard.updateWorldTime();
                playerFastBoard.updatePing();
                playerFastBoard.updatePlayTime(onlinePlayer.playTimeKeyRemaining);

                onlinePlayer.updatePlayTime();
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
        private final int defaultPlayTimeKeyCountdown;
        @Getter
        private int playTimeKeyRemaining;

        public OnlinePlayer(SimpleSkyblock plugin, Player player, PlayerEntity playerEntity) {
            this.plugin = plugin;
            this.player = player;
            this.playerEntity = playerEntity;
            this.fastBoard = new PlayerFastBoard(this.plugin, player, playerEntity);

            this.defaultPlayTimeKeyCountdown = this.plugin.serverConfig.getInt("play_time_key_countdown", 14400);
            var playerConfiguration = PlayerUtils.getPlayerConfiguration(this.plugin, player.getUniqueId());
            this.playTimeKeyRemaining = playerConfiguration.getInt("play_time_key_countdown", this.defaultPlayTimeKeyCountdown);
        }

        private void updatePlayTime() {
            this.playTimeKeyRemaining--;
            if (this.playTimeKeyRemaining < 0) {
                var playTimeKey = PlayTimeKey.create(this.plugin);
                this.player.give(playTimeKey);

                this.player.sendMessage(this.plugin.miniMessage.deserialize("<green>You have received a PlayTime Key</green>"));
                PlayerUtils.playSound(this.player, SoundType.POSITIVE);

                this.playTimeKeyRemaining = this.defaultPlayTimeKeyCountdown;
            }
        }
    }

}
