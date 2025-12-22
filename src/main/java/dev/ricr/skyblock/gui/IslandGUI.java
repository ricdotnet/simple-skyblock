package dev.ricr.skyblock.gui;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.IslandEntity;
import dev.ricr.skyblock.enums.Buttons;
import dev.ricr.skyblock.utils.InventoryUtils;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class IslandGUI implements InventoryHolder, ISimpleSkyblockGUI {
    private final SimpleSkyblock plugin;
    @Getter
    private final Inventory inventory;

    public IslandGUI(SimpleSkyblock plugin, Player player) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Island menu"));

        this.openInventory(player);
    }

    @Override
    public void handleInventoryClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        String buttonType = clicked.getItemMeta()
                .getPersistentDataContainer()
                .get(ServerUtils.GUI_BUTTON_TYPE, PersistentDataType.STRING);

        Buttons button = Buttons.getByLabel(buttonType);
        switch (button) {
            case null -> {
            }
            case Buttons.IslandPrivacy -> this.handleIslandPrivacyClick(player);
            case Buttons.IslandAllowNetherTeleport -> this.handleAllowNetherTeleportClick(player);
            case Buttons.IslandAllowOfflineVisits -> this.handleAllowOfflineVisits(player);
            case Buttons.IslandAllowMobSpawning -> this.handleMobSpawningClick(player);
            case Buttons.IslandShowSeed -> this.handleShowIslandSeedClick(player);
        }
    }

    private void openInventory(Player player) {
        var playersDao = this.plugin.databaseManager.getPlayersDao();
        var islandsDao = this.plugin.databaseManager.getIslandsDao();

        var playerUniqueId = player.getUniqueId()
                .toString();

        IslandEntity playerIsland = null;

        try {
            var playerEntity = playersDao.queryForId(playerUniqueId);
            playerIsland = islandsDao.queryForId(playerEntity.getPlayerId());
        } catch (SQLException e) {
            // ignore for now
        }

        if (playerIsland == null) {
            this.plugin.getLogger()
                    .warning("Player " + player.getName() + " has no island");
            var message = "<red>You do not have an island to manage";
            player.sendMessage(this.plugin.miniMessage.deserialize(message));
            return;
        }

        var islandWorld = this.plugin.worldManager.loadOrCreate(player.getUniqueId(), null, null);

        var isIslandPrivate = playerIsland.isPrivate();
        var islandPrivacyDescription = "Makes your island private and prevents other players from sending visit requests.";
        this.addBooleanButton(isIslandPrivate, 10, Buttons.IslandPrivacy, "Island privacy", islandPrivacyDescription);

        var isIslandAllowNetherTeleport = playerIsland.isAllowNetherTeleport();
        var islandAllowNetherTeleportDescription = "Prevents other players from teleporting to your nether island using your nether portal.";
        this.addBooleanButton(isIslandAllowNetherTeleport, 11, Buttons.IslandAllowNetherTeleport, "Nether teleport", islandAllowNetherTeleportDescription);

        var islandAllowOfflineVisits = playerIsland.isAllowOfflineVisits();
        var islandAllowOfflineVisitsDescription = "Allows other players to visit your island even if you are offline. Also allows them to simply visit without confirmation even when you are online.";
        this.addBooleanButton(islandAllowOfflineVisits, 12, Buttons.IslandAllowOfflineVisits, "Offline visits", islandAllowOfflineVisitsDescription);

        var isDoMobSpawn = islandWorld.getGameRuleValue(GameRule.DO_MOB_SPAWNING);
        var islandDoMobSpawnDescription = "Allows mobs to spawn in your island.";
        this.addBooleanButton(Boolean.TRUE.equals(isDoMobSpawn), 19, Buttons.IslandAllowMobSpawning, "Mob spawning", islandDoMobSpawnDescription);

        var islandSizeIcon = new ItemStack(Material.OAK_PLANKS);
        var defaultSize = this.plugin.serverConfig.getInt("island.starting_border_radius", 60);
        var expansionSize = this.plugin.onlinePlayers.getPlayer(player.getUniqueId()).getExpansionSize();
        var totalSize = (defaultSize + expansionSize) * 2 + 1;
        this.setItemSimpleMeta(islandSizeIcon, String.format("Island size: %sx%s", totalSize, totalSize), null);
        this.inventory.setItem(14, islandSizeIcon);

        var seedButton = new ItemStack(Material.FILLED_MAP);
        var showSeedPrice = this.plugin.serverConfig.getDouble("show-seed-price", 25000);

        this.setItemSimpleMeta(seedButton, String.format("Show seed: %s", ServerUtils.formatMoneyValue(showSeedPrice)), Buttons.IslandShowSeed);
        this.inventory.setItem(15, seedButton);

        InventoryUtils.fillEmptySlots(this.inventory);
    }

    private void addBooleanButton(boolean isTrue, int inventoryPosition, Buttons buttonType, String label, String description) {
        ItemStack booleanButton;

        if (isTrue) {
            booleanButton = new ItemStack(Material.GREEN_TERRACOTTA);
            this.setItemComplexMeta(booleanButton, label, description, true, buttonType);
        } else {
            booleanButton = new ItemStack(Material.RED_TERRACOTTA);
            this.setItemComplexMeta(booleanButton, label, description, false, buttonType);
        }

        this.inventory.setItem(inventoryPosition, booleanButton);
    }

    private void setItemSimpleMeta(ItemStack item, String name, @Nullable Buttons buttonType) {
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));

        if (buttonType != null) {
            meta.getPersistentDataContainer()
                    .set(ServerUtils.GUI_BUTTON_TYPE, PersistentDataType.STRING,
                            buttonType.getLabel());
        }

        item.setItemMeta(meta);
    }

    private void setItemComplexMeta(ItemStack item, String label, String description, boolean state, Buttons buttonType) {
        var meta = item.getItemMeta();
        var itemMetaComponent = Component.text(label, NamedTextColor.LIGHT_PURPLE);

        var listOfLore = new ArrayList<Component>();
        listOfLore.addAll(
                List.of(Component.empty(),
                        Component.text("Enabled:", NamedTextColor.WHITE)
                                .appendSpace()
                                .append(Component.text(
                                        String.valueOf(state),
                                        state ? NamedTextColor.GREEN : NamedTextColor.RED
                                )),
                        Component.text("Click to change", NamedTextColor.GRAY),
                        Component.empty()));
        listOfLore.addAll(ServerUtils.wrapLore(description, 28, NamedTextColor.WHITE));

        meta.displayName(itemMetaComponent);
        meta.lore(listOfLore);

        meta.getPersistentDataContainer()
                .set(ServerUtils.GUI_BUTTON_TYPE, PersistentDataType.STRING,
                        buttonType.getLabel());

        item.setItemMeta(meta);
    }

    private void handleIslandPrivacyClick(Player player) {
        try {
            var island = this.plugin.databaseManager.getIslandsDao().queryForId(player.getUniqueId().toString());
            island.setPrivate(!island.isPrivate());
            this.plugin.databaseManager.getIslandsDao().update(island);

            var privacyUpdated = String.format("<white>Island has been made %s", island.isPrivate() ? "<green>private" : "<red>public");
            player.sendMessage(this.plugin.miniMessage.deserialize(privacyUpdated));
        } catch (SQLException e) {
            // ignore for now
        }

        // refresh only
        this.openInventory(player);
    }

    private void handleAllowNetherTeleportClick(Player player) {
        try {
            var island = this.plugin.databaseManager.getIslandsDao().queryForId(player.getUniqueId().toString());
            island.setAllowNetherTeleport(!island.isAllowNetherTeleport());
            this.plugin.databaseManager.getIslandsDao().update(island);

            var netherTeleportUpdated = String.format("<white>Nether teleport has been %s", island.isAllowNetherTeleport() ? "<green>enabled" : "<red>disabled");
            player.sendMessage(this.plugin.miniMessage.deserialize(netherTeleportUpdated));
        } catch (SQLException e) {
            // ignore for now
        }

        // refresh only
        this.openInventory(player);
    }

    private void handleAllowOfflineVisits(Player player) {
        try {
            var island = this.plugin.databaseManager.getIslandsDao().queryForId(player.getUniqueId().toString());
            island.setAllowOfflineVisits(!island.isAllowOfflineVisits());
            this.plugin.databaseManager.getIslandsDao().update(island);

            var offlineVisitsUpdated = String.format("<white>Offline visits have been %s", island.isAllowOfflineVisits() ? "<green>enabled" : "<red>disabled");
            player.sendMessage(this.plugin.miniMessage.deserialize(offlineVisitsUpdated));
        } catch (SQLException e) {
            // ignore for now
        }

        // refresh only
        this.openInventory(player);
    }

    private void handleShowIslandSeedClick(Player player) {
        try {
            var playerEntity = this.plugin.databaseManager.getPlayersDao().queryForId(player.getUniqueId().toString());
            var showSeedPrice = this.plugin.serverConfig.getDouble("show-seed-price", 25000);

            if (playerEntity.getBalance() < showSeedPrice) {
                player.sendMessage(Component.text("You don't have enough money to show the seed", NamedTextColor.RED));
                return;
            }

            var island = this.plugin.databaseManager.getIslandsDao().queryForId(player.getUniqueId().toString());
            var seed = island.getSeed();

            playerEntity.setBalance(playerEntity.getBalance() - showSeedPrice);
            this.plugin.databaseManager.getPlayersDao().update(playerEntity);

            var viewSeedMessage = String.format("Seed: <green>%s", seed);
            player.sendMessage(this.plugin.miniMessage.deserialize(viewSeedMessage));
        } catch (SQLException e) {
            // ignore for now
        }
    }

    private void handleMobSpawningClick(Player player) {
        var islandWorld = this.plugin.worldManager.loadOrCreate(player.getUniqueId(), null, null);
        var isDoMobSpawn = Boolean.TRUE.equals(islandWorld.getGameRuleValue(GameRule.DO_MOB_SPAWNING));

        islandWorld.setGameRule(GameRule.DO_MOB_SPAWNING, !isDoMobSpawn);

        try {
            var islandEntity = this.plugin.databaseManager.getIslandsDao().queryForId(player.getUniqueId().toString());
            if (islandEntity != null && islandEntity.isHasNether()) {
                var netherIslandWorld = this.plugin.worldManager.loadOrCreate(player.getUniqueId(), World.Environment.NETHER, null);
                netherIslandWorld.setGameRule(GameRule.DO_MOB_SPAWNING, !isDoMobSpawn);
            }
        } catch (SQLException e) {
            // ignore for now
        }

        var mobSpawningUpdated = String.format("<white>Mob spawning as been %s", !isDoMobSpawn ? "<green>enabled" : "<red>disabled");
        player.sendMessage(this.plugin.miniMessage.deserialize(mobSpawningUpdated));

        // refresh only
        this.openInventory(player);
    }
}
