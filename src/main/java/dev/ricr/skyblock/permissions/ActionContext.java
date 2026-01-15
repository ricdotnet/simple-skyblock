package dev.ricr.skyblock.permissions;

import dev.ricr.skyblock.SimpleSkyblock;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.Nullable;

public record ActionContext(
        SimpleSkyblock plugin,
        Player player,
        @Nullable Cancellable cancellable,
        boolean shouldNotify
) {}
