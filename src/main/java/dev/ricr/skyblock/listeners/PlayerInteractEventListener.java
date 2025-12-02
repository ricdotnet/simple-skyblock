package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class PlayerInteractEventListener implements Listener {
    private final SimpleSkyblock plugin;
    private final FileConfiguration serverConfig;

    public PlayerInteractEventListener(SimpleSkyblock plugin, FileConfiguration serverConfig) {
        this.plugin = plugin;
        this.serverConfig = serverConfig;
    }

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        if (event.getItem() == null) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Precedence to non-restricted interact events
        if (item.getType() == Material.ENDER_EYE) {
            double x = (double) this.serverConfig.get("stronghold_location.x");
            double y = (double) this.serverConfig.get("stronghold_location.y");
            double z = (double) this.serverConfig.get("stronghold_location.z");

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

    private void decreaseItemAmount(Player player, ItemStack item) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory()
                    .remove(item);
        }
    }
}
