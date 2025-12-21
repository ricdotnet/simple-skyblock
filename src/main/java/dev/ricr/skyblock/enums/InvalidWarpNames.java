package dev.ricr.skyblock.enums;

import lombok.Getter;

public enum InvalidWarpNames {
    Warp("warp", false),
    Spawn("spawn", false),
    Shop("shop", true),
    TradeHall("trade_hall", true),
    PvPArena("pvp_arena", true),
    PvP("pvp", false),
    End("end", false),
    TheEnd("the_end", false),
    EndPortal("end_portal", true),
    Nether("nether", false),
    TheNether("the_nether", false),
    Create("create", false),
    List("list", false);

    @Getter
    private final String name;
    @Getter
    private final boolean adminOverride;

    InvalidWarpNames(String name, boolean adminOverride) {
        this.name = name;
        this.adminOverride = adminOverride;
    }

    public static InvalidWarpNames getByName(String name) {
        for (InvalidWarpNames warp : values()) {
            if (warp.getName().equalsIgnoreCase(name)) {
                return warp;
            }
        }
        return null;
    }
}
