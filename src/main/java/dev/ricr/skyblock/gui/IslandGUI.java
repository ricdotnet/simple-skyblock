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
            case Buttons.IslandPrivacy -> {
                Dao<Island, String> islandDao = this.plugin.databaseManager.getIslandsDao();
                try {
                    Island island = islandDao.queryForId(player.getUniqueId()
                            .toString());
                    island.setPrivate(!island.isPrivate());
                    islandDao.update(island);
                } catch (SQLException e) {
                    // ignore for now
                }

                // refresh only
                this.openInventory(player);
            }
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

        boolean islandIsPrivate = userIsland.isPrivate();

        ItemStack islandPrivacy;
        if (islandIsPrivate) {
            islandPrivacy = new ItemStack(Material.GREEN_TERRACOTTA);
            this.setItemMeta(islandPrivacy, "Island is private", Buttons.IslandPrivacy);
        } else {
            islandPrivacy = new ItemStack(Material.RED_TERRACOTTA);
            this.setItemMeta(islandPrivacy, "Island is public", Buttons.IslandPrivacy);
        }
        inventory.setItem(10, islandPrivacy);
    }

    private void setItemMeta(ItemStack item, String name, Buttons buttonType) {
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.getPersistentDataContainer()
                .set(ServerUtils.GUI_BUTTON_TYPE, PersistentDataType.STRING,
                        buttonType.getLabel());
        item.setItemMeta(meta);
    }
}
