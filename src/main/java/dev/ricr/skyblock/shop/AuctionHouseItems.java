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

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.AuctionHouse;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuctionHouseItems {
    private final SimpleSkyblock plugin;
    private final int TOTAL_ITEMS_PER_PAGE = 45;

    private List<ItemStack> auctionItems;
    private int totalItems;

    public AuctionHouseItems(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.auctionItems = new ArrayList<>();
    }

    public void loadAuctionItems() {
        this.plugin.getLogger()
                .info("Loading existing auction items");

        Dao<AuctionHouse, Integer> auctionHouseDao = this.plugin.databaseManager.getAuctionHouseDao();

        try {
            List<AuctionHouse> auctionHouseItemsList = auctionHouseDao.queryBuilder()
                    .orderBy("id", false)
                    .query();
            this.totalItems = auctionHouseItemsList.size();

            for (AuctionHouse auctionHouseItem : auctionHouseItemsList) {
                ItemStack item = ItemStack.deserializeBytes(ServerUtils.bytesFromBase64(auctionHouseItem.getItem()));

                buildAndAddMeta(item, auctionHouseItem.getOwnerName(), auctionHouseItem.getPrice());

                auctionItems.add(item);
            }
        } catch (SQLException e) {
            // ignore for now
        }
    }

    public void addItem(ItemStack item) {
        this.auctionItems.addFirst(item);
    }

    public List<ItemStack> getPageOfItems(int page) {
        return this.auctionItems;
    }

    public void buildAndAddMeta(ItemStack item, String ownerName, double price) {
        ItemMeta meta = item.getItemMeta();
        meta.lore(List.of(
                Component.text("> " + ownerName + " <", NamedTextColor.YELLOW),
                Component.empty(),
                Component.text()
                        .content("Price: ")
                        .append(Component.text(String.format("%s%s", ServerUtils.COIN_SYMBOL,
                                        ServerUtils.formatMoneyValue(price)),
                                NamedTextColor.GOLD))
                        .build()
        ));
        item.setItemMeta(meta);
    }

}
