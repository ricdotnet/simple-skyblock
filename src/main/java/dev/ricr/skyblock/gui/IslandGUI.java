package dev.ricr.skyblock.gui;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.Island;
import dev.ricr.skyblock.enums.Buttons;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;

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
            case Buttons.IslandShowSeed -> this.handleShowIslandSeedClick(player);
        }
    }

    private void openInventory(Player player) {
        var userDao = this.plugin.databaseManager.getUsersDao();
        var islandDao = this.plugin.databaseManager.getIslandsDao();

        var playerUniqueId = player.getUniqueId()
                .toString();

        Island userIsland = null;

        try {
            var user = userDao.queryForId(playerUniqueId);
            userIsland = islandDao.queryForId(user.getUserId());
        } catch (SQLException e) {
            // ignore for now
        }

        if (userIsland == null) {
            this.plugin.getLogger()
                    .warning("Player " + player.getName() + " has no island");
            return;
        }

        var isIslandPrivate = userIsland.isPrivate();
        this.addBooleanButton(isIslandPrivate, 10, Buttons.IslandPrivacy, "Island is private", "Island is public");

        var isIslandAllowNetherTeleport = userIsland.isAllowNetherTeleport();
        this.addBooleanButton(isIslandAllowNetherTeleport, 11, Buttons.IslandAllowNetherTeleport, "Disable Nether teleport", "Allow Nether teleport");

        var seedButton = new ItemStack(Material.FILLED_MAP);
        var showSeedPrice = this.plugin.serverConfig.getDouble("show-seed-price", 25000);

        this.setItemMeta(seedButton, String.format("Show seed: %s%s", ServerUtils.COIN_SYMBOL, ServerUtils.formatMoneyValue(showSeedPrice)), Buttons.IslandShowSeed);
        this.inventory.setItem(12, seedButton);
    }

    private void addBooleanButton(boolean isTrue, int inventoryPosition, Buttons label, String nameOn, String nameOff) {
        ItemStack booleanButton;

        if (isTrue) {
            booleanButton = new ItemStack(Material.GREEN_TERRACOTTA);
            this.setItemMeta(booleanButton, nameOn, label);
        } else {
            booleanButton = new ItemStack(Material.RED_TERRACOTTA);
            this.setItemMeta(booleanButton, nameOff, label);
        }

        this.inventory.setItem(inventoryPosition, booleanButton);
    }

    private void setItemMeta(ItemStack item, String name, Buttons buttonType) {
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
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
        } catch (SQLException e) {
            // ignore for now
        }

        // refresh only
        this.openInventory(player);
    }

    private void handleShowIslandSeedClick(Player player) {
        try {
            var user = this.plugin.databaseManager.getUsersDao().queryForId(player.getUniqueId().toString());
            var showSeedPrice = this.plugin.serverConfig.getDouble("show-seed-price", 25000);

            if (user.getBalance() < showSeedPrice) {
                player.sendMessage(Component.text("You don't have enough money to show the seed", NamedTextColor.RED));
                return;
            }

            var island = this.plugin.databaseManager.getIslandsDao().queryForId(player.getUniqueId().toString());
            var seed = island.getSeed();

            user.setBalance(user.getBalance() - showSeedPrice);
            this.plugin.databaseManager.getUsersDao().update(user);

            player.sendMessage(Component.text("Seed:").appendSpace().append(Component.text(seed, NamedTextColor.GREEN)));
        } catch (SQLException e) {
            // ignore for now
        }
    }
}
