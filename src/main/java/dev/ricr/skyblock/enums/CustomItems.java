package dev.ricr.skyblock.enums;

import dev.ricr.skyblock.DisplayNames;
import dev.ricr.skyblock.gui.ChestKeyGUI;
import dev.ricr.skyblock.items.CreeperCoin;
import dev.ricr.skyblock.items.LuckyPickaxe;
import lombok.Getter;

public enum CustomItems {
    CREEPER_COIN(DisplayNames.CREEPER_COIN, CreeperCoin.class),
    LUCKY_PICKAXE(DisplayNames.LUCKY_PICKAXE, LuckyPickaxe.class),
    PLAYTIME_KEY(DisplayNames.PLAYTIME_KEY, ChestKeyGUI.class);

    @Getter
    private final String label;
    @Getter
    private final Class<?> clazz;

    CustomItems(String label, Class<?> clazz) {
        this.label = label;
        this.clazz = clazz;
    }
}
