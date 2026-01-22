package dev.ricr.skyblock.items;

import dev.ricr.skyblock.DisplayNames;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.enums.CustomItems;
import dev.ricr.skyblock.utils.ServerUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Random;

public class LuckyPickaxe {

    private static final Material[] COMMON_ITEMS = {
            Material.COAL,
            Material.RAW_COPPER,
            Material.RAW_IRON,
            Material.RAW_GOLD,
    };

    public static ItemStack create(SimpleSkyblock plugin) {
        var luckyPickaxe = new ItemStack(Material.DIAMOND_PICKAXE, 1);
        luckyPickaxe.addEnchantment(Enchantment.EFFICIENCY, 2);

        var itemMeta = luckyPickaxe.getItemMeta();

        itemMeta.displayName(plugin.miniMessage.deserialize("<!italic><dark_purple>" + DisplayNames.LUCKY_PICKAXE));
        itemMeta.lore(List.of(
                plugin.miniMessage.deserialize("<!italic><white>Use this pickaxe to get ores when mining.")
        ));
        itemMeta.setEnchantmentGlintOverride(true);

        var itemPersistentDataContainer = itemMeta.getPersistentDataContainer();
        itemPersistentDataContainer.set(ServerUtils.CUSTOM_ITEM, PersistentDataType.STRING, CustomItems.LUCKY_PICKAXE.name());
        itemPersistentDataContainer.set(ServerUtils.NO_ANVIL, PersistentDataType.BYTE, (byte) 1);
        itemPersistentDataContainer.set(ServerUtils.NO_ENCHANTMENT, PersistentDataType.BYTE, (byte) 1);

        luckyPickaxe.setItemMeta(itemMeta);

        return luckyPickaxe;
    }

    public static Material getRandom() {
        var random = new Random();
        var r = random.nextInt(100);
        if (r == 1) return Math.random() <= 0.1 ? Material.NETHERITE_SCRAP : Material.DIAMOND;
        else if (r > 1 && r <= 5) return Material.EMERALD;
        else if (r > 5 && r <= 15) return Material.REDSTONE;
        else if (r > 15 && r <= 25) return Material.LAPIS_LAZULI;
        else {
            // there is an equal chance of "75% / n" for all the common items
            return COMMON_ITEMS[(int) (Math.random() * COMMON_ITEMS.length)];
        }
    }
}
