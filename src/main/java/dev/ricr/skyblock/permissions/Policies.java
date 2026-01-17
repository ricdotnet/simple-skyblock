package dev.ricr.skyblock.permissions;

import lombok.Getter;

public class Policies {

    private static final Requirement<ActionContext> ALL = Requirements.permission(PoliciesEnum.ALL.getLabel());

    public static final Requirement<ActionContext> BREAK_BLOCKS = Policies.ALL.or(Requirements.permission(PoliciesEnum.BREAK_BLOCKS.getLabel()));
    public static final Requirement<ActionContext> PLACE_BLOCKS = Policies.ALL.or(Requirements.permission(PoliciesEnum.PLACE_BLOCKS.getLabel()));
    public static final Requirement<ActionContext> KILL_MOBS = Policies.ALL.or(Requirements.permission(PoliciesEnum.KILL_MOBS.getLabel()));
    public static final Requirement<ActionContext> INTERACT_WITH_MOBS = Policies.ALL.or(Requirements.permission(PoliciesEnum.INTERACT_WITH_MOBS.getLabel()));
    public static final Requirement<ActionContext> VILLAGER_TRADING = Policies.ALL.or(Requirements.permission(PoliciesEnum.VILLAGER_TRADING.getLabel()));
    public static final Requirement<ActionContext> PORTAL_TRAVEL = Policies.ALL.or(Requirements.permission(PoliciesEnum.PORTAL_TRAVEL.getLabel()));
    // this should include all inventories, but we can split in the future if needed
    public static final Requirement<ActionContext> OPEN_INVENTORIES = Policies.ALL.or(Requirements.permission(PoliciesEnum.OPEN_INVENTORIES.getLabel()));
    // all doors should be opened, normal doors, trap doors and even fence gates
    public static final Requirement<ActionContext> OPEN_DOORS = Policies.ALL.or(Requirements.permission(PoliciesEnum.OPEN_DOORS.getLabel()));

    public enum PoliciesEnum {
        ALL("all"),
        BREAK_BLOCKS("break_blocks"),
        PLACE_BLOCKS("place_blocks"),
        KILL_MOBS("kill_mobs"),
        INTERACT_WITH_MOBS("interact_with_mobs"),
        VILLAGER_TRADING("villager_trading"),
        PORTAL_TRAVEL("portal_travel"),
        OPEN_INVENTORIES("open_inventories"),
        OPEN_DOORS("open_doors");

        @Getter
        private final String label;

        PoliciesEnum(String label) {
            this.label = label;
        }

        public static PoliciesEnum getByLabel(String label) {
            for (PoliciesEnum policy : values()) {
                if (policy.getLabel().equalsIgnoreCase(label)) {
                    return policy;
                }
            }
            return null;
        }
    }

}
