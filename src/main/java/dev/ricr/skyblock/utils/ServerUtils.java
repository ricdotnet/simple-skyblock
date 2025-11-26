package dev.ricr.skyblock.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.logging.Logger;

public class ServerUtils {
    private static final Logger logger = Logger.getLogger("SimpleSkyblock");

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

    public static String getTextFromComponent(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

}
