package dev.ricr.skyblock.gui;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.items.CreeperCoin;
import dev.ricr.skyblock.shop.VillagerShopItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import java.util.ArrayList;
import java.util.List;

public class VillagerShopGUI {
    private final SimpleSkyblock plugin;
    private final Merchant merchant;
    private final List<VillagerShopItem> shopItemsList;

    public VillagerShopGUI(SimpleSkyblock plugin, String name, Player player, List<VillagerShopItem> shopItemsList, String color) {
        this.plugin = plugin;

        this.merchant = Bukkit.createMerchant(Component.text(name, NamedTextColor.NAMES.value(color)));
        this.shopItemsList = shopItemsList;

        this.openMerchant(player);
    }

    private void openMerchant(Player player) {
        List<MerchantRecipe> recipes = new ArrayList<>();

        for (var item : this.shopItemsList) {
            var recipe = new MerchantRecipe(item.itemStack(), 999);

            var creeperCoinItem = CreeperCoin.create(this.plugin, item.coinAmount());
            recipe.addIngredient(creeperCoinItem);

            if (item.tradeInExtra() != null && item.tradeInExtraAmount() != null) {
                var itemStackTradeIn2 = new ItemStack(item.tradeInExtra(), item.tradeInExtraAmount());
                recipe.addIngredient(itemStackTradeIn2);
            }

            recipes.add(recipe);
        }

        this.merchant.setRecipes(recipes);
        player.openMerchant(this.merchant, true);
    }
}
