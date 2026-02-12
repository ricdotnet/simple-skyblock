package dev.ricr.skyblock.items;

import dev.ricr.skyblock.DisplayNames;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.enums.CustomItems;
import dev.ricr.skyblock.utils.ServerUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.util.List;

public class CreeperCoin {

    public static ItemStack create(SimpleSkyblock plugin, @Nullable Integer amount) {
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

        var itemPersistentDataContainer = itemMeta.getPersistentDataContainer();
        itemPersistentDataContainer.set(ServerUtils.CUSTOM_ITEM, PersistentDataType.STRING, CustomItems.CREEPER_COIN.name());
        itemPersistentDataContainer.set(ServerUtils.NO_ANVIL, PersistentDataType.BYTE, (byte) 1);

        creeperCoin.setItemMeta(itemMeta);

        if (amount != null) {
            creeperCoin.setAmount(amount);
        }

        return creeperCoin;
    }

}
