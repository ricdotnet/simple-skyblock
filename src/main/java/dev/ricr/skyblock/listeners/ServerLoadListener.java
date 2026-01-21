package dev.ricr.skyblock.listeners;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.utils.ServerUtils;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

public class ServerLoadListener implements Listener {
    private final SimpleSkyblock plugin;

    public ServerLoadListener(SimpleSkyblock plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        ServerUtils.setEndPortalTextDisplay(this.plugin);

        this.plugin.loadVillagerShops();
        this.loadKeyChests();
    }

    // TODO: refactor key chest loader
    private void loadKeyChests() {
        this.plugin.getLogger().info("Loading key chests");

        var keyChestsConfig = this.plugin.serverConfig.getMapList("key_chests");
        if (keyChestsConfig.isEmpty()) {
            this.plugin.getLogger().warning("No key chests found in config.yml");
            return;
        }

        for (Map<?, ?> keyChestConfig : keyChestsConfig) {
            var chestName = keyChestConfig.get("name").toString();
            var displayName = keyChestConfig.get("display_name").toString();
            var worldName = keyChestConfig.get("world");
            var x = keyChestConfig.get("x");
            var y = keyChestConfig.get("y");
            var z = keyChestConfig.get("z");

            var world = this.plugin.worldManager.load(worldName.toString());
            var foundBlock = world.getBlockAt((int) x, (int) y, (int) z);

            if (!(foundBlock.getState() instanceof Chest chest)) {
                this.plugin.getLogger().warning(String.format("Could not find key chest %s at %s, %s, %s", chestName, x, y, z));
                continue;
            }

            this.plugin.getLogger().info(String.format("Found key chest %s at %s, %s, %s", chestName, x, y, z));

            var chestPersistentDataContainer = chest.getPersistentDataContainer();
            chestPersistentDataContainer.set(ServerUtils.KEY_CHEST, PersistentDataType.STRING, chestName);
            chest.update(true);

            var location = chest.getLocation();
            location.setX(location.getX() + 0.5);
            location.setY(location.getY() + 1.5);
            location.setZ(location.getZ() + 0.5);
            ServerUtils.setChestKeyTextDisplay(this.plugin, location);
        }
    }
}
