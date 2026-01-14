package dev.ricr.skyblock.enchantments;

import dev.ricr.skyblock.SimpleSkyblock;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import net.kyori.adventure.key.Key;
import org.bukkit.enchantments.Enchantment;

public class PluginEnchantments {

    public static TypedKey<Enchantment> LUCKY_ENCHANTMENT = EnchantmentKeys.create(Key.key("simpleskyblock:lucky"));

    public static void register(BootstrapContext context) {
        context.getLogger().info("Registering custom enchantments");

        LuckyEnchantment.register(context);
    }

    public static Enchantment get(TypedKey<Enchantment> key) {
        return SimpleSkyblock.ENCHANTMENTS.get(key);
    }

}
