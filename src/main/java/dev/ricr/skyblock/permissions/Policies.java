package dev.ricr.skyblock.permissions;

public class Policies {

    private static final Requirement<ActionContext> ALL = Requirements.permission("all");

    public static final Requirement<ActionContext> BREAK_BLOCKS = Policies.ALL.or(Requirements.permission("break_blocks"));
    public static final Requirement<ActionContext> PLACE_BLOCKS = Policies.ALL.or(Requirements.permission("place_blocks"));
    public static final Requirement<ActionContext> KILL_MOBS = Policies.ALL.or(Requirements.permission("kill_mobs"));
    public static final Requirement<ActionContext> VILLAGER_TRADING = Policies.ALL.or(Requirements.permission("villager_trading"));
    public static final Requirement<ActionContext> PORTAL_TRAVEL = Policies.ALL.or(Requirements.permission("portal_travel"));
    public static final Requirement<ActionContext> OPEN_CHESTS = Policies.ALL.or(Requirements.permission("open_chests"));
    public static final Requirement<ActionContext> OPEN_DOORS = Policies.ALL.or(Requirements.permission("open_doors"));

}
