package dev.ricr.skyblock.utils;

import dev.ricr.skyblock.SimpleSkyblock;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CustomItems {

    public static ItemStack createCreeperCoinItem(SimpleSkyblock plugin) {
        var creeperCoin = new ItemStack(Material.PAPER, 1);
        var itemMeta = creeperCoin.getItemMeta();

        itemMeta.getCustomModelDataComponent().setFloats(List.of(1F));
        itemMeta.displayName(plugin.miniMessage.deserialize("<!italic><green>Creeper Coin"));
        itemMeta.lore(List.of(
            plugin.miniMessage.deserialize("<!italic><white>Use this coin to trade in the trade shops.")
        ));
        itemMeta.setEnchantmentGlintOverride(true);
        creeperCoin.setItemMeta(itemMeta);

        return creeperCoin;
    }

}
