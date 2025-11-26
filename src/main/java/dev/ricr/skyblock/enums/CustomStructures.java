package dev.ricr.skyblock.enums;

public enum CustomStructures {
    ISLAND("skyblock_island.nbt"),
    STRONGHOLD("stronghold.nbt");

    public final String label;

    CustomStructures(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
