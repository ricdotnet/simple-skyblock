package dev.ricr.skyblock;

import dev.ricr.skyblock.shop.VillagerShopItem;
import dev.ricr.skyblock.utils.NumberUtils;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VillagerShopManager {
    private final SimpleSkyblock plugin;
    private final Map<UUID, VillagerShop> villagerShops;

    public VillagerShopManager(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.villagerShops = new HashMap<>();

        this.loadVillagerShops();
    }

    public void addVillagerShop(VillagerShop villagerShop) {
        this.villagerShops.put(villagerShop.getShopUniqueId(), villagerShop);
    }

    public VillagerShop getVillagerShop(UUID villagerShopId) {
        return this.villagerShops.get(villagerShopId);
    }

    public void removeVillagerShops() {
        var world = this.plugin.worldManager.load("lobby");

        for (var entity : world.getEntitiesByClass(Villager.class)) {
            var villagerShopEntity = this.villagerShops.get(entity.getUniqueId());

            if (villagerShopEntity != null) {
                this.plugin.getLogger().info(String.format("Removing villager shop %s", villagerShopEntity.getName()));
                entity.remove();
            }
        }
    }

    private void loadVillagerShops() {
        var shopsInConfig = this.plugin.serverConfig.getMapList("villager_shops");

        for (Map<?, ?> shop : shopsInConfig) {
            var shopName = shop.get("name").toString();
            var itemsList = shop.get("items");

            if (itemsList == null) {
                continue;
            }

            var position = (Map<?, ?>) shop.get("position");

            double x = NumberUtils.objectToDouble(position.get("x"));
            double y = NumberUtils.objectToDouble(position.get("y"));
            double z = NumberUtils.objectToDouble(position.get("z"));

            var location = new Location(null, x, y, z, 90, 0);
            var villagerEntity = this.loadOrCreateVillager(shopName, shop.get("color").toString(), location);

            if (villagerEntity == null) {
                this.plugin.getLogger().severe(String.format("Could not load or create villager for shop %s", shopName));
                continue;
            }

            var villagerShop = new VillagerShop(villagerEntity.getUniqueId(), shopName, shop.get("color").toString());

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

    private Villager loadOrCreateVillager(String name, String color, Location spawnLocation) {
        var world = this.plugin.getServer().getWorld("lobby");

        if (world == null) {
            this.plugin.getLogger().severe("The world had not been loaded yet");
            return null;
        }

        spawnLocation.setWorld(world);
        this.plugin.getLogger().info(String.format("Creating villager shop %s", name));

        var villager = world.spawn(spawnLocation, Villager.class);
        villager.getPersistentDataContainer()
                .set(ServerUtils.VILLAGER_SHOP_NAME, PersistentDataType.STRING, name);

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
        private String color;
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
