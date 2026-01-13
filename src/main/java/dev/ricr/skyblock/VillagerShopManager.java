package dev.ricr.skyblock;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.database.VillagerShopEntity;
import dev.ricr.skyblock.shop.VillagerShopItem;
import dev.ricr.skyblock.utils.NumberUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    // TODO: extract shop loading logic to allow for a in-memory loads
    private void loadVillagerShops() {
        var shopsInConfig = this.plugin.serverConfig.getMapList("villager_shops");
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

            for (Map<?, ?> item : (List<Map<?, ?>>) itemsList) {
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

            this.addVillagerShop(villagerShop);
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
