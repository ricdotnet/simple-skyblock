package dev.ricr.skyblock.utils;

import dev.ricr.skyblock.permissions.IslandPermissions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record IslandRecord(UUID owner, int x, int z, IslandPermissions islandPermissions, List<Tuple<String, String>> trustedPlayers, List<Tuple<String, String>> blockedPlayers) {
    public IslandRecord addTrustedPlayer(String playerUniqueId, String playerName) {
        var updated = new ArrayList<>(this.trustedPlayers);
        updated.add(new Tuple<>(playerUniqueId, playerName));
        return new IslandRecord(this.owner(), this.x(), this.z(), this.islandPermissions(), List.copyOf(updated), this.blockedPlayers);
    }

    public IslandRecord removeTrustedPlayer(String playerName) {
        var updated = new ArrayList<>(this.trustedPlayers)
                .stream()
                .filter(trustedPlayer -> !trustedPlayer.getSecond().equals(playerName))
                .toList();
        return new IslandRecord(this.owner(), this.x(), this.z(), this.islandPermissions(), List.copyOf(updated), this.blockedPlayers);
    }

    public IslandRecord addBlockedPlayer(String playerUniqueId, String playerName) {
        var updated = new ArrayList<>(this.blockedPlayers);
        updated.add(new Tuple<>(playerUniqueId, playerName));
        return new IslandRecord(this.owner(), this.x(), this.z(), this.islandPermissions(), this.trustedPlayers, List.copyOf(updated));
    }

    public IslandRecord removeBlockedPlayer(String playerName) {
        var updated = new ArrayList<>(this.blockedPlayers)
                .stream()
                .filter(blockedPlayer -> !blockedPlayer.getSecond().equals(playerName))
                .toList();
        return new IslandRecord(this.owner(), this.x(), this.z(), this.islandPermissions(), this.trustedPlayers, List.copyOf(updated));
    }
}
