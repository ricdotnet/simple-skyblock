package dev.ricr.skyblock.utils;

import dev.ricr.skyblock.SimpleSkyblock;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

public class PlayerUtils {
    static Logger logger = Logger.getLogger("SimpleSkyblock");

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
                .getWorld(config.getString("world")), config.getDouble("x"),
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
}
