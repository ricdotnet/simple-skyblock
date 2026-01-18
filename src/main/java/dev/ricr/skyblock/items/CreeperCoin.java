package dev.ricr.skyblock.items;

import dev.ricr.skyblock.DisplayNames;
import dev.ricr.skyblock.SimpleSkyblock;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CreeperCoin {

    public static ItemStack create(SimpleSkyblock plugin) {
        var creeperCoin = new ItemStack(Material.PAPER, 1);
        var itemMeta = creeperCoin.getItemMeta();

        var customModel = itemMeta.getCustomModelDataComponent();
        customModel.setFloats(List.of(1F));

        itemMeta.setCustomModelDataComponent(customModel);
        itemMeta.displayName(plugin.miniMessage.deserialize("<!italic><green>" + DisplayNames.CREEPER_COIN));
        itemMeta.lore(List.of(
                plugin.miniMessage.deserialize("<!italic><white>Use this coin to trade in the trade shops.")
        ));
        itemMeta.setEnchantmentGlintOverride(true);
        creeperCoin.setItemMeta(itemMeta);

        return creeperCoin;
    }

}
