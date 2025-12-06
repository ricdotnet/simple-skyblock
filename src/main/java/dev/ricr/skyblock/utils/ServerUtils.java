package dev.ricr.skyblock.utils;

import dev.ricr.skyblock.SimpleSkyblock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

public class ServerUtils {
    private static final Logger logger = Logger.getLogger("SimpleSkyblock");

    public static final int ISLAND_SPACING = 300;
    public static final int MIN_STRONGHOLD_LOCATION = -3500;
    public static final int MAX_STRONGHOLD_LOCATION = 3500;

    public static final String COIN_SYMBOL = "$";

    public static final int GAMBLE_MINIMUM_BALANCE = 100;
    public static final int GAMBLE_MAXIMUM_BALANCE = 5000;
    public static final int GAMBLE_COUNTDOWN = 30;

    public static final int AUCTION_HOUSE_MAX_LISTINGS = 10;

    public static final int PLAYER_ISLAND_BORDER_RADIUS = 280;

    // auction house GUI
    public static NamespacedKey AUCTION_HOUSE_ITEM_ID;
    public static final String AUCTION_NEXT_PAGE = "auction_next_page";
    public static final String AUCTION_PREVIOUS_PAGE = "auction_previous_page";
    public static final String AUCTION_REFRESH_BUTTON = "auction_refresh_button";

    // other GUI
    public static NamespacedKey GUI_BUTTON_TYPE;

    public static FileConfiguration loadConfig(File dataFolder) {
        File serverConfig = new File(dataFolder, "config.yml");

        return YamlConfiguration.loadConfiguration(serverConfig);
    }

    public static void saveConfig(FileConfiguration config, File dataFolder) {
        File serverConfig = new File(dataFolder, "config.yml");

        try {
            config.save(serverConfig);
        } catch (Exception e) {
            logger.severe("Failed to save config: " + e.getMessage());
        }
    }

    public static List<Component> getLoreOrEmptyComponentList(ItemMeta meta) {
        return meta.lore() != null ? meta.lore() : new ArrayList<>();
    }

    public static String getTextFromComponent(Component component) {
        return PlainTextComponentSerializer.plainText()
                .serialize(component);
    }

    public static double formatMoneyValue(double value) {
        return Math.round(value * 100.00) / 100.00;
    }

    public static String base64FromBytes(byte[] bytes) {
        return java.util.Base64.getEncoder()
                .encodeToString(bytes);
    }

    public static byte[] bytesFromBase64(String base64) {
        return java.util.Base64.getDecoder()
                .decode(base64);
    }

    public static void initiateNamespacedKeys(SimpleSkyblock plugin) {
        AUCTION_HOUSE_ITEM_ID = new NamespacedKey(plugin, "auction_house_item");
        GUI_BUTTON_TYPE = new NamespacedKey(plugin, "gui_button_type");
    }

}
