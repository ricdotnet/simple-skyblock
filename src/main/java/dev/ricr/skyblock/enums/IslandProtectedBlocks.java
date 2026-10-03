package dev.ricr.skyblock.enums;

import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Set;

public class IslandProtectedBlocks {
    public static final Set<Material> REDSTONE_ITEMS = EnumSet.of(
            Material.STONE_BUTTON,
            Material.OAK_BUTTON,
            Material.BIRCH_BUTTON,
            Material.SPRUCE_BUTTON,
            Material.JUNGLE_BUTTON,
            Material.ACACIA_BUTTON,
            Material.DARK_OAK_BUTTON,
            Material.CRIMSON_BUTTON,
            Material.WARPED_BUTTON,
            Material.LEVER,
            Material.COMPARATOR,
            Material.REPEATER
    );

    public static final Set<Material> DOORS = EnumSet.of(
            Material.OAK_DOOR,
            Material.BIRCH_DOOR,
            Material.BAMBOO_DOOR,
            Material.SPRUCE_DOOR,
            Material.JUNGLE_DOOR,
            Material.ACACIA_DOOR,
            Material.DARK_OAK_DOOR,
            Material.CRIMSON_DOOR,
            Material.WARPED_DOOR,
            Material.OAK_TRAPDOOR,
            Material.BIRCH_TRAPDOOR,
            Material.BAMBOO_TRAPDOOR,
            Material.SPRUCE_TRAPDOOR,
            Material.JUNGLE_TRAPDOOR,
            Material.ACACIA_TRAPDOOR,
            Material.DARK_OAK_TRAPDOOR,
            Material.CRIMSON_TRAPDOOR,
            Material.WARPED_TRAPDOOR,
            Material.OAK_FENCE_GATE,
            Material.BIRCH_FENCE_GATE,
            Material.BAMBOO_FENCE_GATE,
            Material.SPRUCE_FENCE_GATE,
            Material.JUNGLE_FENCE_GATE,
            Material.ACACIA_FENCE_GATE,
            Material.DARK_OAK_FENCE_GATE,
            Material.CRIMSON_FENCE_GATE,
            Material.WARPED_FENCE_GATE
    );
}
