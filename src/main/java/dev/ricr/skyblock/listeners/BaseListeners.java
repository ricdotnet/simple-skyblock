package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.DatabaseChange;
import dev.ricr.skyblock.enums.EventCancellationReasons;
import dev.ricr.skyblock.enums.IslandProtectedBlocks;
import dev.ricr.skyblock.gui.AuctionHouseGUI;
import dev.ricr.skyblock.gui.ConfirmGUI;
import dev.ricr.skyblock.gui.GambleSessionGUI;
import dev.ricr.skyblock.gui.IslandGUI;
import dev.ricr.skyblock.gui.IslandSettingsGUI;
import dev.ricr.skyblock.gui.ItemsListGUI;
import dev.ricr.skyblock.gui.LeaderBoardGUI;
import dev.ricr.skyblock.gui.PlayersListGUI;
import dev.ricr.skyblock.gui.ShopTypeGUI;
import dev.ricr.skyblock.gui.VillagerShopGUI;
import dev.ricr.skyblock.permissions.ActionContext;
import dev.ricr.skyblock.permissions.EventCancellations;
import dev.ricr.skyblock.permissions.Policies;
import dev.ricr.skyblock.utils.Messages;
import dev.ricr.skyblock.utils.PlayerUtils;
import dev.ricr.skyblock.utils.ServerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataType;

public class BaseListeners implements Listener {
    private final SimpleSkyblock plugin;

    public BaseListeners(SimpleSkyblock plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();

        var isUsingLobbyWorld = this.plugin.serverConfig.getBoolean("lobby", true);
        if (!isUsingLobbyWorld) {
            // TODO: implement island creation on join
            return;
        }

        var lobbyWorld = ServerUtils.loadOrCreateLobby();
        player.sendMessage(Component.text("Welcome to SimpleSkyblock!", NamedTextColor.GREEN));

        PlayerUtils.createPlayerEntity(this.plugin, player);
        this.plugin.islandManager.addPlayerIsland(player.getUniqueId());

        // always start in the lobby / spawn world
        player.teleport(new Location(lobbyWorld, 0.5, 65, 0.5));

        this.plugin.onlinePlayers.getPlayer(player.getUniqueId()).getFastBoard().updateWorld("lobby");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();

        this.plugin.onlinePlayers.removePlayer(player.getUniqueId());
        this.plugin.islandManager.removePlayerIsland(player.getUniqueId());

        var world = player.getWorld();
        if (world.getName().equals("lobby")) {
            return;
        }

        this.plugin.worldManager.unload(world);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        var player = event.getEntity();
        var playerFastBoard = this.plugin.onlinePlayers.getPlayer(player.getUniqueId()).getFastBoard();

        if (playerFastBoard == null) {
            return;
        }

        playerFastBoard.updateDeaths();
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        var player = event.getPlayer();
        var islandRecord = this.plugin.islandManager.getIslandRecord(player.getUniqueId());

        if (!event.isBedSpawn() || islandRecord == null) {
            var lobbyWorld = ServerUtils.loadOrCreateLobby();
            player.teleport(new Location(lobbyWorld, 0.5, 65, 0.5));
            return;
        }

        var playerIslandWorld = this.plugin.worldManager.loadOrCreate(player.getUniqueId(), null, null);
        var islandLocation = PlayerUtils.getTpLocation(this.plugin, player.getUniqueId());
        islandLocation.setWorld(playerIslandWorld);

        player.teleport(islandLocation);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        var actionContext = new ActionContext(this.plugin, event.getPlayer(), event);
        if (!Policies.BREAK_BLOCKS.test(actionContext)) {
            EventCancellations.add(event, EventCancellationReasons.NO_PERMISSION);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        var actionContext = new ActionContext(this.plugin, event.getPlayer(), event);
        if (!Policies.PLACE_BLOCKS.test(actionContext)) {
            EventCancellations.add(event, EventCancellationReasons.NO_PERMISSION);
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        var interactedEntity = event.getRightClicked();

        if (interactedEntity instanceof Villager) {
            var actionContext = new ActionContext(this.plugin, event.getPlayer(), event);
            if (!Policies.VILLAGER_TRADING.test(actionContext)) {
                EventCancellations.add(event, EventCancellationReasons.NO_PERMISSION);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }

        // TODO: we could allow pvp with an island rule

        var damagee = event.getEntity();
        var actionContext = new ActionContext(this.plugin, damager, event);
        if (!(damagee instanceof Player) && !Policies.KILL_MOBS.test(actionContext)) {
            EventCancellations.add(event, EventCancellationReasons.NO_PERMISSION);
        }
    }

    @EventHandler
    public void onUseEndPortal(PlayerPortalEvent event) {
        var to = event.getTo().getWorld();
        var player = event.getPlayer();

        event.setCancelled(true);

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
    public void onPlayerInteractWithDoor(PlayerInteractEvent event) {
        var player = event.getPlayer();
        var clickedBlock = event.getClickedBlock();
        var clickedBlockMaterial = clickedBlock == null ? Material.AIR : clickedBlock.getType();

        if (!IslandProtectedBlocks.DOORS.contains(clickedBlockMaterial)) {
            return;
        }

        var actionContext = new ActionContext(this.plugin, player, event);
        if (!Policies.OPEN_DOORS.test(actionContext)) {
            EventCancellations.add(event, EventCancellationReasons.NO_PERMISSION);
        }
    }

    @EventHandler
    public void onEntityUsePortals(EntityPortalEvent event) {
        event.setCancelled(true);

        // TODO: implement entity teleport later
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        Inventory inventory = event.getInventory();
        InventoryType inventoryType = inventory.getType();
        InventoryHolder inventoryHolder = inventory.getHolder();

        if (inventoryType == InventoryType.PLAYER || inventoryType == InventoryType.MERCHANT) {
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
            case VillagerShopGUI ignored -> {
            }
            case PlayersListGUI<?> ignored -> {
            }
            case IslandSettingsGUI ignored -> {
            }
            default -> {
                var actionContext = new ActionContext(this.plugin, player, event);
                if (!Policies.OPEN_INVENTORIES.test(actionContext)) {
                    EventCancellations.add(event, EventCancellationReasons.NO_PERMISSION);
                }
            }
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        var left = event.getInventory().getItem(0);
        if (left == null || left.getType().isAir()) return;

        if (left.getItemMeta().getPersistentDataContainer().has(ServerUtils.NO_ANVIL, PersistentDataType.BYTE)) {
            event.setResult(null);
        }
    }

    @EventHandler
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        var first = event.getInventory().getUpperItem();
        if (first == null) return;

        if (first.getItemMeta().getPersistentDataContainer().has(ServerUtils.NO_ANVIL, PersistentDataType.BYTE)) {
            event.setResult(null);
        }
    }

    @EventHandler
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        var item = event.getItem();

        if (item.getItemMeta().getPersistentDataContainer().has(ServerUtils.NO_ENCHANTMENT, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

}
