package dev.ricr.skyblock.commands;

import dev.ricr.skyblock.SimpleSkyblock;

public class Commands {

    public static void register(SimpleSkyblock plugin) {
        new AdminCommand(plugin).register();
        new IslandCommand(plugin).register();
        new GambleCommand(plugin).register();
        new PayCommand(plugin).register();
        new WarpCommand(plugin).register();
        new LobbyCommand(plugin);
        new BalanceCommand(plugin);
        new ShopCommand(plugin);
        new LeaderboardCommand(plugin);
        new AuctionHouseCommand(plugin);
    }

}
