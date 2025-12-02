package dev.ricr.skyblock.gui;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.enums.ShopType;
import dev.ricr.skyblock.shop.ShopItems;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.AllArgsConstructor;
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

import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class ItemsListGUI implements InventoryHolder, ISimpleSkyblockGUI {
    @Getter
    private final Inventory inventory;
    private final SimpleSkyblock plugin;
    @Getter
    private final ShopType shopType;

    public ItemsListGUI(SimpleSkyblock plugin, Map<Material, ShopItems.PricePair> shopItems, ShopType shopType) {
        this.plugin = plugin;
        this.shopType = shopType;
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Shop"));

        // starting at slot 10 for having a border around
        int slot = 10;
        for (Map.Entry<Material, ShopItems.PricePair> entry : shopItems.entrySet()) {
            while (isBorderSlot(slot)) {
                slot++;
            }

            Material material = entry.getKey();
            ShopItems.PricePair prices = entry.getValue();

            ItemStack item = new ItemStack(material, 1);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                String sellPrice = priceOrNotAvailable(prices.sellPrice());
                String buyPrice = priceOrNotAvailable(prices.buyPrice());

                meta.displayName(Component.text(material.name()));
                meta.lore(List.of(
                        Component.empty(),
                        Component.text()
                                .content("Sell: ")
                                .append(Component.text(sellPrice, "Not available".equals(sellPrice) ?
                                        NamedTextColor.RED : NamedTextColor.GREEN))
                                .build(),
                        Component.text()
                                .content("Buy: ")
                                .append(Component.text(buyPrice, "Not available".equals(buyPrice) ?
                                        NamedTextColor.RED : NamedTextColor.GREEN))
                                .build()
                ));
                item.setItemMeta(meta);
            }

            inventory.setItem(slot++, item);
        }

        ItemStack goBackButton = new ItemStack(Material.BARRIER, 1);
        ItemMeta meta = goBackButton.getItemMeta();
        meta.displayName(Component.text("Go back"));
        goBackButton.setItemMeta(meta);
        inventory.setItem(49, goBackButton);
    }

    @Override
    public void handleInventoryClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        Material material = clicked.getType();
        ShopItems.PricePair prices = null;

        if (material == Material.BARRIER) {
            player.openInventory(new ShopTypeGUI(this.plugin).getInventory());
            return;
        }

        switch (this.getShopType()) {
            case ShopType.Blocks -> prices = ShopItems.BLOCKS.get(material);
            case ShopType.Items -> prices = ShopItems.ITEMS.get(material);
        }

        if (prices == null) {
            return;
        }

        ConfirmGUI confirmGUI = new ConfirmGUI(this.plugin, player, material, prices, this.getShopType());
        player.openInventory(confirmGUI.getInventory());
    }

    private boolean isBorderSlot(int slot) {
        int inventoryWidth = 9;

        int row = slot / inventoryWidth;
        int col = slot % inventoryWidth;

        return col == 0 || col == inventoryWidth - 1 || row == 0 || row == inventoryWidth - 1;
    }

    private String priceOrNotAvailable(double price) {
        if (price == -1) {
            return "Not available";
        } else {
            return String.format("%s%s", ServerUtils.COIN_SYMBOL, price);
        }
    }
}