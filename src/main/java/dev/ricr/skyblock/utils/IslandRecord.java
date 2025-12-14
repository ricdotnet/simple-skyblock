package dev.ricr.skyblock.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record IslandRecord(UUID owner, int x, int z, List<Tuple<String, String>> trustedPlayers) {
    public IslandRecord addTrustedPlayer(String playerUniqueId, String playerName) {
        var updated = new ArrayList<>(trustedPlayers);
        updated.add(new Tuple<>(playerUniqueId, playerName));
        return new IslandRecord(this.owner(), this.x(), this.z(), List.copyOf(updated));
    }

    public IslandRecord removeTrustedPlayer(String playerName) {
        var updated = new ArrayList<>(trustedPlayers)
                .stream()
                .filter(trustedPlayer -> !trustedPlayer.getSecond().equals(playerName))
                .toList();
        return new IslandRecord(this.owner(), this.x(), this.z(), List.copyOf(updated));
    }
}
