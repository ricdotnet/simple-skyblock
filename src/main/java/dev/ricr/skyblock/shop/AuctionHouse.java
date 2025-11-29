package dev.ricr.skyblock.shop;

// <- go previous page
// -> go next page
// ## exit

// Auction House - Page xx/xx
// 00 01 02 03 04 05 06 07 08
// 09 10 11 12 13 14 15 16 17
// 18 19 20 21 22 23 24 25 26
// 27 28 29 30 31 32 33 34 35
// 36 37 38 39 40 41 42 43 44
// <- 46 47 48 ## 50 51 52 ->

import dev.ricr.skyblock.SimpleSkyblock;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AuctionHouse {
    private final SimpleSkyblock plugin;
    private final int TOTAL_ITEMS_PER_PAGE = 45;

    private List<ItemStack> auctionItems;
    private int totalItems;

    public AuctionHouse(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.auctionItems = new ArrayList<>();
    }

    public void loadAuctionItems() {
        this.plugin.getLogger().info("Loading existing auction items");
        // Implement - either read from a serialized file or database items?
    }

    public List<ItemStack> getPageOfItems(int page) {
        return List.of();
    }

}
