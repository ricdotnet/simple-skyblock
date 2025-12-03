package dev.ricr.skyblock.enums;

public enum Buttons {
    IslandPrivacy("island_privacy"),
    BorderVisibility("border_visibility");

    public final String label;

    Buttons(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
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
