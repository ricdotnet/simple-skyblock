package dev.ricr.skyblock.enchantments;

import dev.ricr.skyblock.SimpleSkyblock;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.registry.TypedKey;
import org.bukkit.enchantments.Enchantment;

public class PluginEnchantments {

    public static void register(BootstrapContext context) {
        context.getLogger().info("Registering custom enchantments");
    }

    public static Enchantment get(TypedKey<Enchantment> key) {
        return SimpleSkyblock.ENCHANTMENTS.get(key);
    }

}
