package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.IslandEntity;
import dev.ricr.skyblock.enums.EventCancellationReasons;
import dev.ricr.skyblock.enums.IslandProtectedBlocks;
import dev.ricr.skyblock.enums.SignShopType;
import dev.ricr.skyblock.permissions.ActionContext;
import dev.ricr.skyblock.permissions.EventCancellations;
import dev.ricr.skyblock.permissions.Policies;
import dev.ricr.skyblock.shop.SignShop;
import dev.ricr.skyblock.utils.Messages;
import dev.ricr.skyblock.utils.PlayerUtils;
import dev.ricr.skyblock.utils.ServerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.sql.SQLException;

public class PlayerListeners implements Listener {
    private final SimpleSkyblock plugin;

    public PlayerListeners(SimpleSkyblock plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        var player = event.getPlayer();
        var worldFrom = event.getFrom();
        var worldTo = player.getWorld();

        if (worldTo.getName().equals("lobby")) {
            if (worldFrom.getEnvironment() == World.Environment.THE_END) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.teleport(new Location(worldTo, 0.5, 65, 0.5));
                });
            }

            var message = "<red>-= <gold>lobby island</gold> =-";
            PlayerUtils.showTitleMessage(this.plugin, player, this.plugin.miniMessage.deserialize(message), 20L);

            // try to unload the world that the player teleported from
            this.plugin.worldManager.unload(worldFrom);
            this.plugin.onlinePlayers.getPlayer(player.getUniqueId()).getFastBoard().updateWorld("lobby");
            return;
        }

        if (worldTo.getEnvironment() == World.Environment.THE_END) {
            PlayerUtils.showTitleMessage(this.plugin, player, Messages.THE_END_ISLAND.component(this.plugin), 20L);

            // try to unload the world that the player teleported from
            this.plugin.worldManager.unload(worldFrom);
            this.plugin.onlinePlayers.getPlayer(player.getUniqueId()).getFastBoard().updateWorld("the end");
            return;
        }

        IslandEntity playerIsland = null;
        var islandId = worldTo.getName()
                .replace("islands/", "")
                .replace("_nether", "");
        try {
            playerIsland = this.plugin.databaseManager.getIslandsDao().queryForId(islandId);
        } catch (SQLException e) {
            // ignore for now
        }

        if (playerIsland != null) {
            var islandOwnerUsername = playerIsland.getPlayer().getUsername();
            var message = String.format("<green>-= <gold>%s's island</gold> =-", islandOwnerUsername);

            var fastBoardWorldName = islandOwnerUsername.equals(player.getName()) ? "your island" : String.format("%s's island", islandOwnerUsername);
            this.plugin.onlinePlayers.getPlayer(player.getUniqueId()).getFastBoard().updateWorld(fastBoardWorldName);

            PlayerUtils.showTitleMessage(this.plugin, player, this.plugin.miniMessage.deserialize(message), 20L);
        } else {
            this.plugin.onlinePlayers.getPlayer(player.getUniqueId()).getFastBoard().updateWorld("unknown");
        }

        this.plugin.worldManager.loadOrCreate(player.getUniqueId(), worldTo.getEnvironment(), null);
        this.plugin.worldManager.unload(worldFrom);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        Block clickedBlock = event.getClickedBlock();
        Material clickedBlockMaterial = clickedBlock == null ? Material.AIR : clickedBlock.getType();

        var shouldStopIslandInteraction = this.plugin.islandManager.shouldStopIslandInteraction(player);

        if (clickedBlockMaterial == Material.DRAGON_EGG) {
            event.setCancelled(true);
            player.sendMessage(Messages.DRAGON_EGG_BELONGS_TO_SERVER.component(this.plugin));
            return;
        }

        if (event.getAction() == Action.PHYSICAL) {
            if (shouldStopIslandInteraction) {
                event.setCancelled(true);
                player.sendMessage(Messages.CANNOT_DO_THAT_HERE.component(this.plugin));
            }
            return;
        }

        if (IslandProtectedBlocks.BLOCKS.contains(clickedBlockMaterial) && shouldStopIslandInteraction) {
            event.setCancelled(true);
            player.sendMessage(Messages.CANNOT_DO_THAT_HERE.component(this.plugin));
            return;
        }

        if (clickedBlock != null && clickedBlock.getState() instanceof Sign sign) {
            var signShopTypeString = sign.getPersistentDataContainer().get(ServerUtils.SIGN_SHOP_TYPE, PersistentDataType.STRING);
            if (signShopTypeString == null) {
                return;
            }

            new SignShop(this.plugin, event, sign, SignShopType.getByLabel(signShopTypeString));
            return;
        }

        ItemStack itemInHand = event.getItem();
        if (itemInHand == null) {
            return;
        }
        Material material = itemInHand.getType();

        // Precedence to non-restricted interact events
        if (itemInHand.getType() == Material.ENDER_EYE) {
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

            decreaseItemAmount(player, itemInHand);

            return;
        }

        if (itemInHand.getType() == Material.FIREWORK_ROCKET) {
            // ignore fireworks
            return;
        }

        if (material.isEdible()) {
            // ignore food
            return;
        }

        if (shouldStopIslandInteraction) {
            player.sendMessage(Messages.CANNOT_DO_THAT_HERE.component(this.plugin));
            event.setCancelled(true);
            return;
        }

        if (itemInHand.getType() == Material.BUCKET) {
            if (clickedBlock == null) return;

            if (clickedBlock.getType() == Material.OBSIDIAN && event.getAction()
                    .isRightClick()) {
                decreaseItemAmount(player, itemInHand);
                player.getInventory()
                        .addItem(new ItemStack(Material.LAVA_BUCKET));
                clickedBlock.setType(Material.AIR);
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1f, 1f);
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        var player = event.getPlayer();

        var actionContext = new ActionContext(this.plugin, player, event);
        if (!Policies.VILLAGER_TRADING.test(actionContext)) {
            EventCancellations.add(event, EventCancellationReasons.NO_PERMISSION);
            return;
        }
    }

    @EventHandler
    public void onSignChangeEvent(SignChangeEvent event) {
        new SignShop(this.plugin, event);
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

