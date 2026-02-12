package dev.ricr.skyblock.utils;

import dev.ricr.skyblock.permissions.IslandPermissions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// TODO: convert to a normal class because this needs heavy mutation
public record IslandRecord(UUID owner, int x, int z, Boolean isPrivate, Boolean allowOfflineVisits,
                           IslandPermissions islandPermissions, List<Tuple<String, String>> trustedPlayers,
                           List<Tuple<String, String>> blockedPlayers) {

    public IslandRecord updateIslandPrivacy() {
        var updated = Boolean.FALSE.equals(this.isPrivate());
        return new IslandRecord(this.owner(), this.x(), this.z(), updated, this.allowOfflineVisits(), this.islandPermissions(), this.trustedPlayers, this.blockedPlayers);
    }

    public IslandRecord updateAllowOfflineVisits() {
        var updated = Boolean.FALSE.equals(this.allowOfflineVisits());
        return new IslandRecord(this.owner(), this.x(), this.z(), this.isPrivate(), updated, this.islandPermissions(), this.trustedPlayers, this.blockedPlayers);
    }

    public IslandRecord addTrustedPlayer(String playerUniqueId, String playerName) {
        var updated = new ArrayList<>(this.trustedPlayers);
        updated.add(new Tuple<>(playerUniqueId, playerName));
        return new IslandRecord(this.owner(), this.x(), this.z(), this.isPrivate(), this.allowOfflineVisits(), this.islandPermissions(), List.copyOf(updated), this.blockedPlayers);
    }

    public IslandRecord removeTrustedPlayer(String playerName) {
        var updated = new ArrayList<>(this.trustedPlayers)
                .stream()
                .filter(trustedPlayer -> !trustedPlayer.getSecond().equals(playerName))
                .toList();
        return new IslandRecord(this.owner(), this.x(), this.z(), this.isPrivate(), this.allowOfflineVisits(), this.islandPermissions(), List.copyOf(updated), this.blockedPlayers);
    }

    public IslandRecord addBlockedPlayer(String playerUniqueId, String playerName) {
        var updated = new ArrayList<>(this.blockedPlayers);
        updated.add(new Tuple<>(playerUniqueId, playerName));
        return new IslandRecord(this.owner(), this.x(), this.z(), this.isPrivate(), this.allowOfflineVisits(), this.islandPermissions(), this.trustedPlayers, List.copyOf(updated));
    }

    public IslandRecord removeBlockedPlayer(String playerName) {
        var updated = new ArrayList<>(this.blockedPlayers)
                .stream()
                .filter(blockedPlayer -> !blockedPlayer.getSecond().equals(playerName))
                .toList();
        return new IslandRecord(this.owner(), this.x(), this.z(), this.isPrivate(), this.allowOfflineVisits(), this.islandPermissions(), this.trustedPlayers, List.copyOf(updated));
    }
}
