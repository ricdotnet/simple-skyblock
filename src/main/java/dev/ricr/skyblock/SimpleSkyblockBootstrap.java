package dev.ricr.skyblock;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;

public class SimpleSkyblockBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {
        System.out.println("Some logging in here...");
    }

}
