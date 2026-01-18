package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.enums.CustomStructures;
import dev.ricr.skyblock.enums.EventCancellationReasons;
import dev.ricr.skyblock.permissions.ActionContext;
import dev.ricr.skyblock.permissions.EventCancellations;
import dev.ricr.skyblock.permissions.Policies;
import dev.ricr.skyblock.utils.ServerUtils;
import dev.ricr.skyblock.utils.StructureUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;
import java.util.UUID;

public class IslandListeners implements Listener {
    private final SimpleSkyblock plugin;

    public IslandListeners(SimpleSkyblock plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onShopSignBreak(BlockBreakEvent event) {
        var player = event.getPlayer();
        var brokenBlock = event.getBlock();

        var actionContext = new ActionContext(this.plugin, player, event);
        if (!Policies.BREAK_BLOCKS.test(actionContext)) {
            EventCancellations.add(event, EventCancellationReasons.NO_PERMISSION);
            return;
        }

        if (!(brokenBlock.getBlockData() instanceof WallSign)) {
            return;
        }

        var sign = (Sign) event.getBlock().getState();
        var shopOwnerId = sign.getPersistentDataContainer().get(ServerUtils.SIGN_SHOP_OWNER, PersistentDataType.STRING);

        var isShop = sign.getPersistentDataContainer().get(ServerUtils.SIGN_SHOP_TYPE, PersistentDataType.STRING) != null;
        var isShopOwner = shopOwnerId != null && shopOwnerId.equals(player.getUniqueId().toString());

        if (isShop && isShopOwner) {
            player.sendMessage(Component.text("Your Sign Trade shop has been destroyed", NamedTextColor.GREEN));
        }
    }

    @EventHandler
    public void onUseNetherPortal(PlayerPortalEvent event) {
        var from = event.getFrom().getWorld();
        var to = event.getFrom().getWorld();
        var player = event.getPlayer();

        // we need to block all portal travels
        // all are handled by the plugin
        event.setCancelled(true);

        if (!from.getName().startsWith("islands/")) return;
        if (from.getEnvironment() != World.Environment.NORMAL) return;
        if (to.getEnvironment() == World.Environment.THE_END) return;

        var actionContext = new ActionContext(this.plugin, player, event);
        if (!Policies.PORTAL_TRAVEL.test(actionContext)) {
            EventCancellations.add(event, EventCancellationReasons.NO_PERMISSION);
            return;
        }

        var targetIslandId = from.getName()
                .replace("islands/", "")
                .replace("_nether", "");

        try {
            var island = this.plugin.databaseManager.getIslandsDao().queryForId(targetIslandId);

            if (!player.getUniqueId().toString().equals(targetIslandId) && !island.isHasNether()) {
                player.sendMessage(Component.text("The island owner has not generated a Nether island yet", NamedTextColor.RED));
                return;
            }

            var seed = island.getSeed();
            var netherWorld = this.plugin.worldManager.loadOrCreate(UUID.fromString(targetIslandId), World.Environment.NETHER, seed);

            if (!island.isHasNether()) {
                StructureUtils.placeStructure(this.plugin, new Location(netherWorld, -4, 61, -4), CustomStructures.NETHER_ISLAND);
                island.setHasNether(true);
                this.plugin.databaseManager.getIslandsDao().update(island);

                netherWorld.setDifficulty(Difficulty.HARD);
                netherWorld.save();
            }

            player.teleport(new Location(netherWorld, 0.5, 64, 0.5, 180f, 0f));
        } catch (SQLException e) {
            // ignore for now
            player.sendMessage(Component.text("Something went wrong when trying to go to the Nether", NamedTextColor.RED));
            return;
        }

        player.setNoDamageTicks(20 * 10);
        player.sendMessage(Component.text("Welcome to the Nether", NamedTextColor.GREEN));
    }

}
