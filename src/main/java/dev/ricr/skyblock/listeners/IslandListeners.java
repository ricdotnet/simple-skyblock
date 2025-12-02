package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

@AllArgsConstructor
public class IslandListeners implements Listener {
    private final SimpleSkyblock plugin;

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (this.plugin.islandManager.shouldStopIslandInteraction(player)) {
            player.sendMessage(Component.text("You cannot do that here", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (this.plugin.islandManager.shouldStopIslandInteraction(player)) {
            player.sendMessage(Component.text("You cannot do that here", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() == null) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

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

        if (this.plugin.islandManager.shouldStopIslandInteraction(player)) {
            player.sendMessage(Component.text("You cannot do that here",
                    NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        if (item.getType() == Material.BUCKET) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock == null) return;

            if (clickedBlock.getType() == Material.OBSIDIAN && event.getAction()
                    .isRightClick()) {
                decreaseItemAmount(player, item);
                player.getInventory()
                        .addItem(new ItemStack(Material.LAVA_BUCKET));
                clickedBlock.setType(Material.AIR);
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1f, 1f);
            }

            return;
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (this.plugin.islandManager.shouldStopIslandInteraction(player)) {
            player.sendMessage(Component.text("You cannot do that here",
                    NamedTextColor.RED));
            event.setCancelled(true);
            return;
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
