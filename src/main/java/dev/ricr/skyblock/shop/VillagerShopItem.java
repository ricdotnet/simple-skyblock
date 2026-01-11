package dev.ricr.skyblock.shop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public record VillagerShopItem(ItemStack itemStack, double buyingPrice, Material tradeInItem1, Material tradeInItem2, int amountItem1, Integer amountItem2) {
}
