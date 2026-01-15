package dev.ricr.skyblock.enchantments;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.Random;

public class LuckyEnchantment {

    private static final Material[] COMMON_ITEMS = {
            Material.COAL,
            Material.RAW_COPPER,
            Material.RAW_IRON,
            Material.RAW_GOLD,
    };

    public static void register(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.compose().newHandler(event ->
                event.registry().register(
                        EnchantmentKeys.create(PluginEnchantments.LUCKY_ENCHANTMENT),
                        b -> {
                            b.description(Component.text("Lucky"))
                                    .supportedItems(event.getOrCreateTag(ItemTypeTagKeys.PICKAXES))
                                    .anvilCost(3)
                                    .weight(1)
                                    .maxLevel(1)
                                    .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(30, 10))
                                    .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(60, 10))
                                    .activeSlots(EquipmentSlotGroup.ANY);
                        }
                )));
    }

    public static Material getRandom() {
        var random = new Random();
        var r = random.nextInt(100);
        if (r == 1) return Material.DIAMOND;
        else if (r > 1 && r <= 5) return Material.EMERALD;
        else if (r > 5 && r <= 15) return Material.REDSTONE;
        else if (r > 15 && r <= 25) return Material.LAPIS_LAZULI;
        else {
            // there is an equal chance of "75% / n" for all the common items
            return COMMON_ITEMS[(int) (Math.random() * COMMON_ITEMS.length)];
        }
    }
}
