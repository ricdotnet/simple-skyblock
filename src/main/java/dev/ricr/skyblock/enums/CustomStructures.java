package dev.ricr.skyblock.enums;

import lombok.Getter;

@Getter
public enum CustomStructures {
    ISLAND("skyblock_island.nbt"),
    STRONGHOLD("stronghold.nbt");

    public final String label;

    CustomStructures(String label) {
        this.label = label;
    }

}
