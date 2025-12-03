package dev.ricr.skyblock.gui;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.Island;
import dev.ricr.skyblock.database.User;
import dev.ricr.skyblock.enums.Buttons;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
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
    private final Dao<Island, String> islandDao;
    @Getter
    private final Inventory inventory;

    public IslandGUI(SimpleSkyblock plugin, Player player) {
        this.plugin = plugin;
        this.islandDao = plugin.databaseManager.getIslandsDao();
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
            case Buttons.BorderVisibility -> this.handleBorderVisibilityClick(player);
        }
    }

    private void openInventory(Player player) {
        Dao<User, String> userDao = this.plugin.databaseManager.getUsersDao();
        Dao<Island, String> islandDao = this.plugin.databaseManager.getIslandsDao();

        String playerUniqueId = player.getUniqueId()
                .toString();

        Island userIsland = null;

        try {
            User user = userDao.queryForId(playerUniqueId);
            userIsland = islandDao.queryForId(user.getUserId());
        } catch (SQLException e) {
            // ignore for now
        }

        if (userIsland == null) {
            this.plugin.getLogger()
                    .warning("Player " + player.getName() + " has no island");
            return;
        }

        boolean isIslandPrivate = userIsland.isPrivate();
        addBooleanButton(isIslandPrivate, 10, Buttons.IslandPrivacy, "Island is private", "Island is public");

        boolean isBorderOn = true;
        addBooleanButton(isBorderOn, 11, Buttons.BorderVisibility, "Border is visible", "Border is hidden");

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
            Island island = this.islandDao.queryForId(player.getUniqueId()
                    .toString());
            island.setPrivate(!island.isPrivate());
            islandDao.update(island);
        } catch (SQLException e) {
            // ignore for now
        }

        // refresh only
        this.openInventory(player);
    }

    private void handleBorderVisibilityClick(Player player) {
        try {
            Island island = this.islandDao.queryForId(player.getUniqueId()
                    .toString());
            island.setBorderVisible(!island.isBorderVisible());
            islandDao.update(island);
        } catch (SQLException e) {
            // ignore for now
        }

        // refresh only
        this.openInventory(player);
    }
}
