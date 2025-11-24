package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class PlayerUseListener implements Listener {
    private final SimpleSkyblock plugin;
    private final FileConfiguration serverConfig;

    public PlayerUseListener(SimpleSkyblock plugin, FileConfiguration serverConfig) {
        this.plugin = plugin;
        this.serverConfig = serverConfig;
    }

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        if (event.getItem() == null) {
            return;
        }

        ItemStack item = event.getItem();

        if (item.getType() == Material.ENDER_EYE) {
            double x = (double) this.serverConfig.get("stronghold_location.x");
            double y = (double) this.serverConfig.get("stronghold_location.y");
            double z = (double) this.serverConfig.get("stronghold_location.z");

            Player player = event.getPlayer();
            Location eyeStart = player.getEyeLocation();
            Location strongholdLocation = new Location(event.getPlayer().getWorld(), x, y, z);

            Vector direction = strongholdLocation.toVector().subtract(eyeStart.toVector()).normalize().multiply(0.5);
            direction.setY(0.3); // upward arc

            Snowball projectile = player.launchProjectile(Snowball.class);
            projectile.setVelocity(direction);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_LAUNCH, 1f, 1f);

            // Particle trail while moving
            Bukkit.getScheduler().runTaskTimer(this.plugin, task -> {
                if (!projectile.isValid()) task.cancel();
                else projectile.getWorld().spawnParticle(Particle.END_ROD, projectile.getLocation(), 1, 0,0,0, 0);
            }, 0, 1);

            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().remove(item);
            }
        }
    }
}
