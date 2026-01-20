package dev.ricr.skyblock.items;

import dev.ricr.skyblock.DisplayNames;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.enums.CustomItems;
import dev.ricr.skyblock.utils.ServerUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class PlayTimeKey {

    public static ItemStack create(SimpleSkyblock plugin) {
        var playTimeKey = new ItemStack(Material.TRIAL_KEY, 1);
        var itemMeta = playTimeKey.getItemMeta();

        itemMeta.displayName(plugin.miniMessage.deserialize("<!italic><dark_purple>" + DisplayNames.PLAYTIME_KEY));
        itemMeta.lore(List.of(
                plugin.miniMessage.deserialize("<!italic><white>Use this key to exchange playtime for rewards.")
        ));
        itemMeta.setEnchantmentGlintOverride(true);

        var itemPersistentDataContainer = itemMeta.getPersistentDataContainer();
        itemPersistentDataContainer.set(ServerUtils.CUSTOM_ITEM, PersistentDataType.STRING, CustomItems.PLAYTIME_KEY.name());
        itemPersistentDataContainer.set(ServerUtils.NO_ANVIL, PersistentDataType.BYTE, (byte) 1);

        playTimeKey.setItemMeta(itemMeta);

        return playTimeKey;
    }
}
