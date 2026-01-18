package dev.ricr.skyblock.enums;

import lombok.Getter;

@Getter
public enum Buttons {
    IslandPrivacy("island_privacy"),
    IslandAllowNetherTeleport("island_allow_nether_teleport"),
    IslandAllowOfflineVisits("island_allow_offline_visits"),
    IslandAllowMobSpawning("island_allow_mob_spawning"),
    IslandShowSeed("island_show_seed"),
    IslandTrustedPlayersList("island_trusted_players_list"),
    IslandBlockedPlayersList("island_blocked_players_list"),
    ModifyIslandSettings("modify_island_settings"),
    BreakBlocksButton("break_blocks"),
    PlaceBlocksButton("place_blocks"),
    KillMobsButton("kill_mobs"),
    InteractWithMobsButton("interact_with_mobs"),
    VillagerTradingButton("villager_trading"),
    PortalTravelButton("portal_travel"),
    OpenInventoriesButton("open_inventories"),
    OpenDoorsButton("open_doors");

    public final String label;

    Buttons(String label) {
        this.label = label;
    }

    public static Buttons getByLabel(String label) {
        for (Buttons button : values()) {
            if (button.label.equalsIgnoreCase(label)) {
                return button;
            }
        }
        return null;
    }
}
