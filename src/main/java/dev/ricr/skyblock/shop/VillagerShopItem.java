package dev.ricr.skyblock.shop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public record VillagerShopItem(ItemStack itemStack, int coinAmount, Material tradeInExtra, Integer tradeInExtraAmount) {
}
