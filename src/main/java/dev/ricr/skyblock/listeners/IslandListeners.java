package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.enums.CustomStructures;
import dev.ricr.skyblock.gui.AuctionHouseGUI;
import dev.ricr.skyblock.gui.ConfirmGUI;
import dev.ricr.skyblock.gui.GambleSessionGUI;
import dev.ricr.skyblock.gui.ItemsListGUI;
import dev.ricr.skyblock.gui.LeaderBoardGUI;
import dev.ricr.skyblock.gui.ShopTypeGUI;
import dev.ricr.skyblock.utils.ServerUtils;
import dev.ricr.skyblock.utils.StructureUtils;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.File;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Set;

@AllArgsConstructor
public class IslandListeners implements Listener {
    private final SimpleSkyblock plugin;

    private static final Set<Material> PROTECTED_BLOCKS = EnumSet.of(
            Material.OAK_DOOR,
            Material.BIRCH_DOOR,
            Material.SPRUCE_DOOR,
            Material.JUNGLE_DOOR,
            Material.ACACIA_DOOR,
            Material.DARK_OAK_DOOR,
            Material.CRIMSON_DOOR,
            Material.WARPED_DOOR,
            Material.OAK_TRAPDOOR,
            Material.BIRCH_TRAPDOOR,
            Material.SPRUCE_TRAPDOOR,
            Material.JUNGLE_TRAPDOOR,
            Material.ACACIA_TRAPDOOR,
            Material.DARK_OAK_TRAPDOOR,
            Material.CRIMSON_TRAPDOOR,
            Material.WARPED_TRAPDOOR,
            Material.STONE_BUTTON,
            Material.OAK_BUTTON,
            Material.BIRCH_BUTTON,
            Material.SPRUCE_BUTTON,
            Material.JUNGLE_BUTTON,
            Material.ACACIA_BUTTON,
            Material.DARK_OAK_BUTTON,
            Material.CRIMSON_BUTTON,
            Material.WARPED_BUTTON,
            Material.LEVER
    );

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

        if (this.plugin.islandManager.shouldStopIslandInteraction(player)) {
            player.sendMessage(Component.text("You cannot do that here", NamedTextColor.RED));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        Block clickedBlock = event.getClickedBlock();
        Material clickedBlockMaterial = clickedBlock == null ? Material.AIR : clickedBlock.getType();

        if (this.plugin.islandManager.shouldStopIslandInteraction(player) && PROTECTED_BLOCKS.contains(clickedBlockMaterial)) {
            player.sendMessage(Component.text("You cannot do that here", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        Material material = item.getType();

        // Precedence to non-restricted interact events
        if (item.getType() == Material.ENDER_EYE) {
            Double x = (Double) this.plugin.serverConfig.get("stronghold_location.x");
            Double y = (Double) this.plugin.serverConfig.get("stronghold_location.y");
            Double z = (Double) this.plugin.serverConfig.get("stronghold_location.z");

            if (x == null || y == null || z == null) {
                this.plugin.getLogger()
                        .warning("Stronghold location not found");
                player.sendMessage(Component.text("Stronghold location not found", NamedTextColor.RED));
                return;
            }

            Location eyeStart = player.getEyeLocation();
            Location strongholdLocation = new Location(event.getPlayer()
                    .getWorld(), x, y, z);

            Vector direction = strongholdLocation.toVector()
                    .subtract(eyeStart.toVector())
                    .normalize()
                    .multiply(0.5);
            direction.setY(0.3); // upward arc

            Snowball projectile = player.launchProjectile(Snowball.class);
            projectile.setVelocity(direction);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_LAUNCH, 1f, 1f);

            // Particle trail while moving
            Bukkit.getScheduler()
                    .runTaskTimer(this.plugin, task -> {
                        if (!projectile.isValid()) task.cancel();
                        else projectile.getWorld()
                                .spawnParticle(Particle.END_ROD, projectile.getLocation(), 1, 0, 0, 0, 0);
                    }, 0, 1);

            decreaseItemAmount(player, item);

            return;
        }

        if (item.getType() == Material.FIREWORK_ROCKET) {
            // ignore fireworks
            return;
        }

        if (material.isEdible()) {
            // ignore food
            return;
        }

        if (this.plugin.islandManager.shouldStopIslandInteraction(player)) {
            player.sendMessage(Component.text("You cannot do that here", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        if (item.getType() == Material.BUCKET) {
            if (clickedBlock == null) return;

            if (clickedBlock.getType() == Material.OBSIDIAN && event.getAction()
                    .isRightClick()) {
                decreaseItemAmount(player, item);
                player.getInventory()
                        .addItem(new ItemStack(Material.LAVA_BUCKET));
                clickedBlock.setType(Material.AIR);
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1f, 1f);
            }

        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

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
        var player = event.getPlayer();

        // we want to block all portal interactions
        event.setCancelled(true);
        if (!from.getName().startsWith("islands/") || from.getEnvironment() != World.Environment.NORMAL || this.plugin.islandManager.shouldStopIslandInteraction(player)) {
            player.sendMessage(Component.text("You cannot go through portals here", NamedTextColor.RED));
            return;
        }

        try {
            var island = this.plugin.databaseManager.getIslandsDao().queryForId(player.getUniqueId().toString());
            var seed = island.getSeed();
            var netherWorld = ServerUtils.loadOrCreateWorld(event.getPlayer(), World.Environment.NETHER, seed);

            if (!island.isHasNether()) {
                StructureUtils.placeStructure(this.plugin, new Location(netherWorld, 0, 64, 0), CustomStructures.NETHER_ISLAND);
                island.setHasNether(true);
                this.plugin.databaseManager.getIslandsDao().update(island);
            }

            player.teleport(new Location(netherWorld, 0.5, 70, 2.5));
        } catch (SQLException e) {
            // ignore for now
            player.sendMessage(Component.text("Something went wrong when trying to go to the Nether", NamedTextColor.RED));
        }

        player.sendMessage(Component.text("Welcome to the Nether", NamedTextColor.GREEN));
    }

    private void decreaseItemAmount(Player player, ItemStack item) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory()
                    .remove(item);
        }
    }
}
