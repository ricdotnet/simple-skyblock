package dev.ricr.skyblock.listeners;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.DatabaseChange;
import dev.ricr.skyblock.database.IslandEntity;
import dev.ricr.skyblock.database.PlayerEntity;
import dev.ricr.skyblock.enums.IslandProtectedBlocks;
import dev.ricr.skyblock.enums.SignShopType;
import dev.ricr.skyblock.shop.SignShop;
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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
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
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();

        var isUsingLobbyWorld = this.plugin.serverConfig.getBoolean("lobby", true);
        if (!isUsingLobbyWorld) {
            // TODO: implement island creation on join
            return;
        }

        var lobbyWorld = ServerUtils.loadOrCreateLobby();
        player.sendMessage(Component.text("Welcome to SimpleSkyblock!", NamedTextColor.GREEN));

        this.createPlayerEntity(player);
        this.plugin.islandManager.addPlayerIsland(player.getUniqueId());

        // always start in the lobby / spawn world
        player.teleport(new Location(lobbyWorld, 0.5, 65, 0.5));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();

        this.plugin.onlinePlayers.removePlayer(player.getUniqueId());

        var world = player.getWorld();
        if (world.getName().equals("lobby")) {
            return;
        }

        this.plugin.worldManager.unload(world);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (!event.isBedSpawn()) {
            this.plugin.getLogger()
                    .info(String.format("Player %s does not have a bed, sending to lobby", player.getName()));

            var islandRecord = this.plugin.islandManager.getIslandRecord(player.getUniqueId());
            if (islandRecord == null) {
                var lobbyWorld = ServerUtils.loadOrCreateLobby();
                player.teleport(new Location(lobbyWorld, 0.5, 65, 0.5));
                return;
            }

            var islandLocation = PlayerUtils.getTpLocation(this.plugin, player.getUniqueId());
            player.teleport(islandLocation);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        var player = event.getPlayer();
        var worldFrom = event.getFrom();
        var worldTo = player.getWorld();

        if (worldTo.getName().equals("lobby") || worldTo.getEnvironment() == World.Environment.THE_END) {
            var message = "<red>-= <gold>lobby island</gold> =-";
            PlayerUtils.showTitleMessage(this.plugin, player, this.plugin.miniMessage.deserialize(message), 20L);

            // try to unload the world that the player teleported from
            this.plugin.worldManager.unload(worldFrom);
            return;
        }

        IslandEntity playerIsland = null;
        var islandId = worldTo.getName()
                .replace("islands/", "")
                .replace("nether_", "");
        try {
            playerIsland = this.plugin.databaseManager.getIslandsDao().queryForId(islandId);
        } catch (SQLException e) {
            // ignore for now
        }

        if (playerIsland != null) {
            var message = String.format("<green>-= <gold>%s's island</gold> =-", playerIsland.getPlayer().getUsername());
            PlayerUtils.showTitleMessage(this.plugin, player, this.plugin.miniMessage.deserialize(message), 20L);
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

        if (event.getAction() == Action.PHYSICAL) {
            if (shouldStopIslandInteraction) {
                event.setCancelled(true);
                player.sendMessage(Component.text("You cannot do that here", NamedTextColor.RED));
            }
            return;
        }

        if (IslandProtectedBlocks.BLOCKS.contains(clickedBlockMaterial) && shouldStopIslandInteraction) {
            player.sendMessage(Component.text("You cannot do that here", NamedTextColor.RED));
            event.setCancelled(true);
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
            player.sendMessage(Component.text("You cannot do that here", NamedTextColor.RED));
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
        Player player = event.getPlayer();

        if (this.plugin.islandManager.shouldStopIslandInteraction(player)) {
            player.sendMessage(Component.text("You cannot do that here", NamedTextColor.RED));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();

        if (player.isOp() && ServerUtils.isOpOverride()) {
            return;
        }

        if (message.equalsIgnoreCase("/seed") || message.contains("/locate")) {
            event.setCancelled(true);
            player.sendMessage(
                    Component.text("You are not allowed to use this command", NamedTextColor.RED)
            );
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        var player = event.getEntity();
        var playerFastBoard = this.plugin.onlinePlayers.getFastBoards().get(player.getUniqueId());

        if (playerFastBoard == null) {
            return;
        }

        playerFastBoard.updateDeaths();
    }

    @EventHandler
    public void onSignChangeEvent(SignChangeEvent event) {
        new SignShop(this.plugin, event);
    }

    private void createPlayerEntity(Player player) {
        Dao<PlayerEntity, String> playersDao = this.plugin.databaseManager.getPlayersDao();

        var playerUniqueId = player.getUniqueId();

        try {
            var playerEntity = playersDao.queryForId(playerUniqueId.toString());

            if (playerEntity != null) {
                this.plugin.getLogger()
                        .info(String.format("Player %s already joined before. Skipping initialization of player entity.",
                                player.getName()));

                this.plugin.onlinePlayers.addPlayer(playerUniqueId, playerEntity);
                return;
            }

            playerEntity = new PlayerEntity();
            playerEntity.setPlayerId(playerUniqueId.toString());
            playerEntity.setUsername(player.getName());
            playerEntity.setBalance(100.0d);

            this.plugin.onlinePlayers.addPlayer(playerUniqueId, playerEntity);

            var playerCreateOrUpdate = new DatabaseChange.PlayerCreateOrUpdate(playerEntity);
            this.plugin.databaseChangesAccumulator.add(playerCreateOrUpdate);
        } catch (SQLException e) {
            // ignore for now
        }
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

