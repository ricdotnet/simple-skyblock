package dev.ricr.skyblock.utils;

import dev.ricr.skyblock.SimpleSkyblock;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

public class PlayerUtils {
    static final Logger logger = Logger.getLogger("SimpleSkyblock");

    public static ItemStack getPlayerHead(UUID playerUniqueId, String... name) {
        logger.info("Getting player head for " + playerUniqueId);

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUniqueId);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        String displayName = name.length > 0 ? name[0] : Objects.requireNonNull(offlinePlayer.getName(), "Name cannot" +
                " be null!");

        if (meta != null) {
            meta.setOwningPlayer(offlinePlayer);
            meta.displayName(Component.text(displayName));
            head.setItemMeta(meta);
        }

        return head;
    }

    public static Location getPlayerIslandLocation(SimpleSkyblock plugin, Player player) {
        UUID playerUniqueId = player.getUniqueId();

        File playerFile = new File(plugin.getDataFolder(), playerUniqueId + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

        return new Location(plugin.getServer()
                .getWorld(Objects.requireNonNull(config.getString("world"))), config.getDouble("x"),
                config.getDouble("y"), config.getDouble(
                "z"));
    }

    public static int getAllItemsInInventoryOfItem(Player player, Material item) {
        return Arrays.stream(player.getInventory()
                        .getContents())
                .map(itemInInventory -> {
                    if (itemInInventory != null && itemInInventory.getType() == item) {
                        return itemInInventory.getAmount();
                    }
                    return 0;
                })
                .reduce(0, Integer::sum);
    }

    public static FileConfiguration getPlayerConfiguration(SimpleSkyblock plugin, UUID playerUniqueId) {
        File playerFile = new File(plugin.getDataFolder(), playerUniqueId + ".yml");
        return YamlConfiguration.loadConfiguration(playerFile);
    }

    public static void savePlayerConfiguration(FileConfiguration playerConfig, File playerConfigFile) {
        try {
            playerConfig.save(playerConfigFile);
        } catch (Exception e) {
            logger.severe("Failed to save config: " + e.getMessage());
        }
    }

    public static void saveTpLocation(SimpleSkyblock plugin, Player player, Location location) {
        var playerConfig = PlayerUtils.getPlayerConfiguration(plugin, player.getUniqueId());

        playerConfig.set("tp.x", location.getX());
        playerConfig.set("tp.y", location.getY());
        playerConfig.set("tp.z", location.getZ());
        playerConfig.set("tp.yaw", location.getYaw());
        playerConfig.set("tp.pitch", location.getPitch());

        var playerConfigFile = new File(plugin.getDataFolder(), String.format("%s.yml", player.getUniqueId()));
        PlayerUtils.savePlayerConfiguration(playerConfig, playerConfigFile);
    }

    public static Location getTpLocation(SimpleSkyblock plugin, Player player) {
        var playerConfig = PlayerUtils.getPlayerConfiguration(plugin, player.getUniqueId());

        double x = NumberUtils.objectToDouble(playerConfig.get("tp.x"));
        double y = NumberUtils.objectToDouble(playerConfig.get("tp.y"));
        double z = NumberUtils.objectToDouble(playerConfig.get("tp.z"));
        float yaw = NumberUtils.objectToFloat(playerConfig.get("tp.yaw"));
        float pitch = NumberUtils.objectToFloat(playerConfig.get("tp.pitch"));

        // TODO: this is not ideal here
        World world = ServerUtils.loadOrCreateWorld(player);

        return new Location(world, x, y, z, yaw, pitch);
    }
}
