package dev.ricr.skyblock.enchantments;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.EquipmentSlotGroup;

public class LuckyEnchantment {

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

}
