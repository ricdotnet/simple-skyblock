package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.DatabaseChange;
import dev.ricr.skyblock.permissions.ActionContext;
import dev.ricr.skyblock.permissions.Policies;
import dev.ricr.skyblock.utils.Messages;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.AllArgsConstructor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerPortalEvent;

@AllArgsConstructor
public class BaseListeners implements Listener {
    private final SimpleSkyblock plugin;

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        var actionContext = new ActionContext(this.plugin, event.getPlayer(), event, true);
        if (!Policies.BREAK_BLOCKS.test(actionContext)) {
            return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        var actionContext = new ActionContext(this.plugin, event.getPlayer(), event, true);
        if (!Policies.PLACE_BLOCKS.test(actionContext)) {
            return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        var entityType = event.getRightClicked();

        var actionContext = new ActionContext(this.plugin, event.getPlayer(), event, true);
        if ((entityType instanceof Villager) && !Policies.VILLAGER_TRADING.test(actionContext)) {
            return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }

        // TODO: we could allow pvp with an island rule

        var damagee = event.getEntity();
        var actionContext = new ActionContext(this.plugin, damager, event, true);
        if (!(damagee instanceof Player) && !Policies.KILL_MOBS.test(actionContext)) {
            return;
        }
    }

    @EventHandler
    public void onUseEndPortal(PlayerPortalEvent event) {
        var to = event.getTo().getWorld();
        var player = event.getPlayer();

        if (to.getEnvironment() != World.Environment.THE_END) {
            return;
        }

        if (ServerUtils.isOpOverride() && player.isOp()) {
            return;
        }

        var playerEntity = this.plugin.onlinePlayers.getPlayer(player.getUniqueId()).getPlayerEntity();
        var endPortalPrice = this.plugin.serverConfig.getInt("end_portal_price", 100000);

        if (playerEntity.getBalance() < endPortalPrice) {
            event.setCancelled(true);
            player.sendMessage(Messages.INSUFFICIENT_END_PORTAL_BALANCE.component(this.plugin, ServerUtils.formatMoneyValue(endPortalPrice - playerEntity.getBalance())));
            return;
        }

        var newBalance = playerEntity.getBalance() - endPortalPrice;
        playerEntity.setBalance(newBalance);

        var playerCreateOrUpdate = new DatabaseChange.PlayerCreateOrUpdate(playerEntity);
        this.plugin.databaseChangesAccumulator.add(playerCreateOrUpdate);
    }

    @EventHandler
    public void onEntityUsePortals(EntityPortalEvent event) {
        event.setCancelled(true);

        // TODO: implement entity teleport later
    }

}
