package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.enums.SoundType;
import dev.ricr.skyblock.utils.CustomItems;
import dev.ricr.skyblock.utils.PlayerUtils;
import dev.ricr.skyblock.utils.ServerUtils;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PiglinBarterEvent;

public class BarterListener implements Listener {
    private final SimpleSkyblock plugin;

    public BarterListener(SimpleSkyblock plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPiglinBarter(PiglinBarterEvent event) {
        var piglin = event.getEntity();
        var target = piglin.getTarget();

        var barteredItems = event.getOutcome();

        if (Math.random() < (ServerUtils.CREEPER_COIN_CHANCE / 100)) {
            var coinAmount = this.coinAmount();

            if (target instanceof Player player) {
                player.sendMessage(this.plugin.miniMessage.deserialize("<green>You got <coin_amount>",
                        Placeholder.unparsed("coin_amount", coinAmount == 1 ? "1 Creeper Coin." : coinAmount + " Creeper Coins.")
                ));
                PlayerUtils.playSound(player, SoundType.POSITIVE);
            }

            barteredItems.clear();
            var creeperCoinItem = CustomItems.createCreeperCoinItem(this.plugin);
            barteredItems.add(creeperCoinItem);
        }
    }

    private int coinAmount() {
        double r = Math.random();
        if (r < 0.05) return 3;
        if (r < 0.05 + 0.20) return 2;
        return 1;
    }
}
