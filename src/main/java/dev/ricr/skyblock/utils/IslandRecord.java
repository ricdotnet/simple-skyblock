package dev.ricr.skyblock.utils;

import java.util.Set;
import java.util.UUID;

public record IslandRecord(UUID owner, int x, int z, Set<UUID> trustedPlayers) {
}
