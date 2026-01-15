package dev.ricr.skyblock.permissions;

public class Policies {

    private static final Requirement<ActionContext> ALL = Requirements.permission("all");

    public static final Requirement<ActionContext> BREAK_BLOCKS = Policies.ALL.or(Requirements.permission("break_blocks"));
    public static final Requirement<ActionContext> PLACE_BLOCKS = Policies.ALL.or(Requirements.permission("place_blocks"));
    public static final Requirement<ActionContext> KILL_MOBS = Policies.ALL.or(Requirements.permission("kill_mobs"));
    public static final Requirement<ActionContext> INTERACT_WITH_MOBS = Policies.ALL.or(Requirements.permission("interact_with_mobs"));
    public static final Requirement<ActionContext> VILLAGER_TRADING = Policies.ALL.or(Requirements.permission("villager_trading"));
    public static final Requirement<ActionContext> PORTAL_TRAVEL = Policies.ALL.or(Requirements.permission("portal_travel"));
    // this should include all inventories, but we can split in the future if needed
    public static final Requirement<ActionContext> OPEN_INVENTORIES = Policies.ALL.or(Requirements.permission("open_inventories"));
    // all doors should be opened, normal doors, trap doors and even fence gates
    public static final Requirement<ActionContext> OPEN_DOORS = Policies.ALL.or(Requirements.permission("open_doors"));

}
