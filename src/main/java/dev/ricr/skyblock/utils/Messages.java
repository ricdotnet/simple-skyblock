package dev.ricr.skyblock.utils;

import dev.ricr.skyblock.SimpleSkyblock;
import net.kyori.adventure.text.Component;

public enum Messages {
    CANNOT_DO_THAT_HERE("<red>You cannot do that here"),
    PVP_NOT_ALLOWED("<red>You cannot do PVP here"),
    THE_END_ISLAND("<red>-= <light_purple>the end</light_purple> =-"),
    DRAGON_EGG_BELONGS_TO_SERVER("<red>The <light_purple>Dragon Egg</light_purple> belongs to the server!"),
    INSUFFICIENT_END_PORTAL_BALANCE("<red>You don't have enough money to go through the end portal.<newline>You need an extra <gold>%s</gold>");

    public final String message;

    Messages(String message) {
        this.message = message;
    }

    public Component component(SimpleSkyblock plugin, Object ...parts) {
        if (parts.length > 0) {
            return plugin.miniMessage.deserialize(String.format(message, parts));
        }
        return plugin.miniMessage.deserialize(message);
    }
}
