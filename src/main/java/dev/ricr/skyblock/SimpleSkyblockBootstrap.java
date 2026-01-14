package dev.ricr.skyblock;

import dev.ricr.skyblock.enchantments.PluginEnchantments;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;

public class SimpleSkyblockBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {
        PluginEnchantments.register(context);
    }

}
