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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuctionHouseItems {
    private final SimpleSkyblock plugin;
    private final int TOTAL_ITEMS_PER_PAGE = 45;
    private final Dao<AuctionHouse, Integer> auctionHouseDao;

    private long totalItems;

    public AuctionHouseItems(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.auctionHouseDao = this.plugin.databaseManager.getAuctionHouseDao();
    }

    public List<ItemStack> getPageOfItems(int page) {
        this.plugin.getLogger()
                .info("Loading existing auction items");

        int offset = (page - 1) * TOTAL_ITEMS_PER_PAGE;
        List<ItemStack> pageItems = new ArrayList<>();

        try {
            List<AuctionHouse> auctionHouseItemsList = auctionHouseDao.queryBuilder()
                    .orderBy("id", false)
                    .limit((long) this.TOTAL_ITEMS_PER_PAGE)
                    .offset((long) offset)
                    .query();

            for (AuctionHouse auctionHouseItem : auctionHouseItemsList) {
                ItemStack item = ItemStack.deserializeBytes(ServerUtils.bytesFromBase64(auctionHouseItem.getItem()));
                buildAndAddMeta(auctionHouseItem.getId(), item, auctionHouseItem.getOwnerName(),
                        auctionHouseItem.getPrice());
                pageItems.add(item);
            }
        } catch (SQLException e) {
            // ignore for now
        }

        return pageItems;
    }

    public long getTotalPages() {
        try {
            this.totalItems = auctionHouseDao.countOf();
        } catch (SQLException e) {
            // ignore for now
        }

        return (int) Math.ceil((double) this.totalItems / TOTAL_ITEMS_PER_PAGE);
    }

    public void buildAndAddMeta(int itemId, ItemStack item, String ownerName, double price) {
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

        meta.getPersistentDataContainer()
                .set(SimpleSkyblock.AUCTION_HOUSE_ITEM_ID, PersistentDataType.INTEGER, itemId);

        item.setItemMeta(meta);
    }

}
