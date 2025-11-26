package dev.ricr.skyblock.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PlayerUtils {
    public static ItemStack getPlayerHead(UUID playerUniqueId, double balance) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUniqueId);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setOwningPlayer(offlinePlayer);
            meta.displayName(Component.text(Objects.requireNonNull(offlinePlayer.getName())));
            meta.lore(List.of(Component.text("Balance: $" + balance)));
            head.setItemMeta(meta);
        }

        return head;
    }

}
