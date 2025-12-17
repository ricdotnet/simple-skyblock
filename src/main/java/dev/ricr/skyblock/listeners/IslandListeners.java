package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.enums.CustomStructures;
import dev.ricr.skyblock.gui.AuctionHouseGUI;
import dev.ricr.skyblock.gui.ConfirmGUI;
import dev.ricr.skyblock.gui.GambleSessionGUI;
import dev.ricr.skyblock.gui.IslandGUI;
import dev.ricr.skyblock.gui.ItemsListGUI;
import dev.ricr.skyblock.gui.LeaderBoardGUI;
import dev.ricr.skyblock.gui.ShopTypeGUI;
import dev.ricr.skyblock.utils.ServerUtils;
import dev.ricr.skyblock.utils.StructureUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;

public class IslandListeners implements Listener {
    private final SimpleSkyblock plugin;

    public IslandListeners(SimpleSkyblock plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (this.plugin.islandManager.shouldStopIslandInteraction(player)) {
            player.sendMessage(Component.text("You cannot do that here", NamedTextColor.RED));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if ((event.getBlock().getBlockData() instanceof WallSign)) {
            var sign = (Sign) event.getBlock().getState();
            var shopOwnerId = sign.getPersistentDataContainer().get(ServerUtils.SIGN_SHOP_OWNER, PersistentDataType.STRING);

            var isShop = sign.getPersistentDataContainer().get(ServerUtils.SIGN_SHOP_TYPE, PersistentDataType.STRING) != null;
            var isShopOwner = shopOwnerId != null && shopOwnerId.equals(player.getUniqueId().toString());

            if (isShop && isShopOwner) {
                player.sendMessage(Component.text("Your Sign Trade shop has been destroyed", NamedTextColor.GREEN));
            }
        }

        if (this.plugin.islandManager.shouldStopIslandInteraction(player)) {
            player.sendMessage(Component.text("You cannot do that here", NamedTextColor.RED));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        Inventory inventory = event.getInventory();
        InventoryType inventoryType = inventory.getType();
        InventoryHolder inventoryHolder = inventory.getHolder();

        if (inventoryType == InventoryType.PLAYER) {
            return;
        }

        switch (inventoryHolder) {
            case null -> {
            }
            case ShopTypeGUI ignored -> {
            }
            case ItemsListGUI ignored -> {
            }
            case ConfirmGUI ignored -> {
            }
            case LeaderBoardGUI ignored -> {
            }
            case GambleSessionGUI ignored -> {
            }
            case AuctionHouseGUI ignored -> {
            }
            case IslandGUI ignored -> {
            }
            default -> {
                if (this.plugin.islandManager.shouldStopIslandInteraction(player)) {
                    player.sendMessage(Component.text("You cannot do that here", NamedTextColor.RED));
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (this.plugin.islandManager.shouldStopIslandInteraction(player)) {
            player.sendMessage(Component.text("You cannot do that here", NamedTextColor.RED));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        var from = event.getFrom().getWorld();
        var to = event.getTo().getWorld();
        var player = event.getPlayer();

        // we want to block all portal interactions
        event.setCancelled(true);

        if (to.getEnvironment() == World.Environment.THE_END) {
            player.sendMessage(Component.text("The End", NamedTextColor.DARK_PURPLE)
                    .appendSpace()
                    .append(Component.text("is not supported yet", NamedTextColor.RED))
            );
            return;
        }

        if (!from.getName().startsWith("islands/") || from.getEnvironment() != World.Environment.NORMAL || this.plugin.islandManager.shouldStopNetherTeleport(player)) {
            player.sendMessage(Component.text("You cannot go through portals here", NamedTextColor.RED));
            return;
        }

        try {
            var island = this.plugin.databaseManager.getIslandsDao().queryForId(player.getUniqueId().toString());
            var seed = island.getSeed();
            var netherWorld = ServerUtils.loadOrCreateWorld(event.getPlayer().getUniqueId(), World.Environment.NETHER, seed);

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
        }

        player.sendMessage(Component.text("Welcome to the Nether", NamedTextColor.GREEN));
    }

}
