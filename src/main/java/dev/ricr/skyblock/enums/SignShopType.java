package dev.ricr.skyblock.enums;

import lombok.Getter;

@Getter
public enum SignShopType {
    Trade("Trade"),
    Shop("Shop");

    public final String label;

    SignShopType(String label) {
        this.label = label;
    }

    public static SignShopType getByLabel(String label) {
        for (SignShopType signShopType : values()) {
            if (signShopType.label.equalsIgnoreCase(label)) {
                return signShopType;
            }
        }
        return null;
    }
}
