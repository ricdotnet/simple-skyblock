package dev.ricr.skyblock.utils;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.VillagerShopEntity;
import dev.ricr.skyblock.shop.VillagerShopItem;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.SQLException;
import java.util.*;

public class VillagerShopManager {
    private final SimpleSkyblock plugin;
    private final Map<UUID, VillagerShop> villagerShops;

    private final Dao<VillagerShopEntity, String> villagerShopsDao;

    public VillagerShopManager(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.villagerShops = new HashMap<>();

        this.villagerShopsDao = plugin.databaseManager.getVillagerShopsDao();

        this.loadVillagerShops();
    }

    public void addVillagerShop(VillagerShop villagerShop) {
        this.villagerShops.put(villagerShop.getShopUniqueId(), villagerShop);
    }

    public VillagerShop getVillagerShop(UUID villagerShopId) {
        return this.villagerShops.get(villagerShopId);
    }

    public Optional<Map.Entry<UUID, VillagerShop>> getVillagerShop(String shopName) {
        return this.villagerShops.entrySet()
                .stream()
                .filter(v -> shopName.equals(v.getValue().getName()))
                .findFirst();
    }

    private void loadVillagerShops() {
        var dataFolder = plugin.getDataFolder();
        var shopConfigFile = new File(dataFolder, "shop.yml");
        var shopConfig = YamlConfiguration.loadConfiguration(shopConfigFile);

        var shopsInConfig = shopConfig.getMapList("villager_shops");
        List<VillagerShopEntity> villagerShopEntities = new ArrayList<>();

        try {
            villagerShopEntities = this.villagerShopsDao.queryForAll();
        } catch (SQLException e) {
            // ignore for now
        }

        for (Map<?, ?> shop : shopsInConfig) {
            var shopName = shop.get("name").toString();
            var itemsList = shop.get("items");

            if (itemsList == null) {
                continue;
            }

            var villagerShopEntity = villagerShopEntities
                    .stream()
                    .filter(ve -> ve.getName().equals(shopName))
                    .findFirst();

            if (villagerShopEntity.isEmpty()) {
                var position = (Map<?, ?>) shop.get("position");
                double x = NumberUtils.objectToDouble(position.get("x"));
                double y = NumberUtils.objectToDouble(position.get("y"));
                double z = NumberUtils.objectToDouble(position.get("z"));

                var location = new Location(null, x, y, z, 90, 0);
                var villagerEntity = this.spawnNewVillager(shopName, shop.get("color").toString(), location);

                try {
                    assert villagerEntity != null;
                    villagerShopEntity = java.util.Optional.of(new VillagerShopEntity(shopName, villagerEntity.getUniqueId().toString()));
                    this.villagerShopsDao.create(villagerShopEntity.get());
                } catch (SQLException e) {
                    // ignore for now
                }
            }

            var villagerShop = new VillagerShop(UUID.fromString(villagerShopEntity.get().getVillagerShopId()), shopName, shop.get("color").toString());
            this.loadShopItems(shopName, villagerShop, (List<Map<?, ?>>) itemsList);

            this.addVillagerShop(villagerShop);
        }
    }

    public void reloadVillagerShops() {
        var dataFolder = plugin.getDataFolder();
        var shopConfigFile = new File(dataFolder, "shop.yml");
        var shopConfig = YamlConfiguration.loadConfiguration(shopConfigFile);

        var shopsInConfig = shopConfig.getMapList("villager_shops");

        for (Map<?, ?> shop : shopsInConfig) {
            var shopName = shop.get("name").toString();
            var itemsList = shop.get("items");

            var villagerShopOptional = this.getVillagerShop(shopName);
            if (villagerShopOptional.isEmpty()) {
                this.plugin.getLogger().warning(String.format("Villager shop %s does not exist in memory. Maybe has not been loaded when the server started?", shopName));
                continue;
            }

            var villagerShop = villagerShopOptional.get().getValue();
            villagerShop.getItems().clear();

            this.loadShopItems(shopName, villagerShop, (List<Map<?, ?>>) itemsList);
        }
    }

    public void resetVillagerShops() {
        List<VillagerShopEntity> villagerShopEntities;
        try {
            villagerShopEntities = this.villagerShopsDao.queryForAll();
        } catch (SQLException e) {
            // ignore for now
            throw new RuntimeException("Could not load villager shop entities:" + e.getMessage());
        }

        var lobbyWorld = Bukkit.getWorld("lobby");

        for (var villagerShopEntity : villagerShopEntities) {
            var villagerEntity = lobbyWorld.getEntity(UUID.fromString(villagerShopEntity.getVillagerShopId()));
            if (villagerEntity == null) {
                this.plugin.getLogger().warning(String.format("Villager entity for villager shop %s was not found in the world", villagerShopEntity.getName()));
                continue;
            }

            try {
                this.villagerShopsDao.delete(villagerShopEntity);
                villagerEntity.remove();
            } catch (SQLException | UnsupportedOperationException e) {
                // ignore for now
                this.plugin.getLogger().severe(
                        String.format("Could not delete or remove villager shop / villager entity %s:" + e.getMessage(),
                                villagerShopEntity.getName())
                );
                continue;
            }
        }

        this.loadVillagerShops();
    }

    private void loadShopItems(String shopName, VillagerShop villagerShop, List<Map<?, ?>> itemsList) {
        for (Map<?, ?> item : itemsList) {
            var materialName = item.get("material").toString();
            var itemAmount = item.get("amount");

            if (itemAmount == null) {
                this.plugin.getLogger().warning(String.format("Item %s in villager shop %s does not have an amount, will reset to 1", materialName, shopName));
                itemAmount = "1";
            }

            var material = Material.getMaterial(materialName);
            if (material == null) {
                this.plugin.getLogger().warning(String.format("Invalid material %s in villager shop %s", materialName, shopName));
                continue;
            }

            var coinAmount = Integer.parseInt(item.get("coin_amount").toString());
            var tradeInExtra = item.get("trade_in_extra");
            var tradeInExtraAmount = item.get("trade_in_extra_amount");

            var itemStack = new ItemStack(material, Integer.parseInt(itemAmount.toString()));

            if (tradeInExtra != null && tradeInExtraAmount != null) {
                villagerShop.addItem(
                        new VillagerShopItem(itemStack, coinAmount, Material.getMaterial(tradeInExtra.toString()), Integer.parseInt(tradeInExtraAmount.toString()))
                );
            } else {
                villagerShop.addItem(new VillagerShopItem(itemStack, coinAmount, null, null));
            }
        }
    }

    private Villager spawnNewVillager(String name, String color, Location spawnLocation) {
        var world = this.plugin.worldManager.load("lobby");

        if (world == null) {
            this.plugin.getLogger().severe("The world had not been loaded yet");
            return null;
        }

        spawnLocation.setWorld(world);
        this.plugin.getLogger().info(String.format("Creating villager shop %s", name));

        var villager = world.spawn(spawnLocation, Villager.class);
        // There is an issue with the PDC in which it does not keep entity state when the server restarts
//        villager.getPersistentDataContainer()
//                .set(ServerUtils.VILLAGER_SHOP_NAME, PersistentDataType.STRING, name);

        villager.setProfession(Villager.Profession.NITWIT);
        villager.setVillagerLevel(1);
        villager.customName(Component.text(name, NamedTextColor.NAMES.value(color)));
        villager.setCustomNameVisible(true);
        villager.setInvulnerable(true);
        villager.setPersistent(true);
        villager.setRemoveWhenFarAway(false);
        villager.setCollidable(false);

        var speed = villager.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(0.0D);
        }

        return villager;
    }

    public static class VillagerShop {
        @Getter
        private final UUID shopUniqueId;
        @Getter
        private final String name;
        @Getter
        private final String color;
        @Getter
        private final List<VillagerShopItem> items = new ArrayList<>();

        public VillagerShop(UUID shopUniqueId, String name, String color) {
            this.shopUniqueId = shopUniqueId;
            this.name = name;
            this.color = color;
        }

        public void addItem(VillagerShopItem shopItem) {
            this.items.add(shopItem);
        }
    }

}
