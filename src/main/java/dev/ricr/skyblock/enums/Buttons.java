package dev.ricr.skyblock.enums;

import lombok.Getter;

@Getter
public enum Buttons {
    IslandPrivacy("island_privacy"),
    IslandAllowNetherTeleport("island_allow_nether_teleport"),
    IslandAllowOfflineVisits("island_allow_offline_visits"),
    IslandShowSeed("island_show_seed");

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
