package dev.ricr.skyblock.commands;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.AuctionHouse;
import dev.ricr.skyblock.database.User;
import dev.ricr.skyblock.gui.AuctionHouseGUI;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

@AllArgsConstructor
public class AuctionHouseCommand implements CommandExecutor {
    private final SimpleSkyblock plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by players");
            return true;
        }

        if (args.length > 1) {
            player.sendMessage(Component.text()
                    .content("Use /auctionhouse to open the auction house GUI")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.newline())
                    .append(Component.text("Or use /auctionhouse <price> to place an auction for the item you are " +
                            "holding", NamedTextColor.YELLOW))
                    .build());

            return true;
        }

        if (args.length == 0) {
            AuctionHouseGUI auctionHouseGUI = new AuctionHouseGUI(this.plugin);
            player.openInventory(auctionHouseGUI.getInventory());

            return true;
        }

        double price = Double.parseDouble(args[0]);

        if (price <= 0) {
            player.sendMessage(Component.text("You cannot place an auction for a negative or zero price",
                    NamedTextColor.RED));

            return true;
        }

        ItemStack itemInHand = player.getInventory()
                .getItemInMainHand();
        ItemStack clonedItem = itemInHand.clone();

        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage(Component.text("You must be holding an item to place an auction for it",
                    NamedTextColor.RED));
            return true;
        }

        Dao<AuctionHouse, Integer> auctionHouseDao = this.plugin.databaseManager.getAuctionHouseDao();
        Dao<User, String> usersDao = this.plugin.databaseManager.getUsersDao();

        try {
            User userSelling = usersDao.queryForId(player.getUniqueId()
                    .toString());

            long playerListingsCount = auctionHouseDao.queryBuilder()
                    .where()
                    .eq("user_id", userSelling.getUserId())
                    .countOf();

            if (playerListingsCount >= ServerUtils.AUCTION_HOUSE_MAX_LISTINGS) {
                player.sendMessage(Component.text("You cannot place more than 10 auctions",
                        NamedTextColor.RED));
                return true;
            }

            AuctionHouse auctionHouse = new AuctionHouse();
            auctionHouse.setUser(userSelling);
            auctionHouse.setOwnerName(player.getName());
            auctionHouse.setPrice(price);
            auctionHouse.setItem(ServerUtils.base64FromBytes(itemInHand.serializeAsBytes()));

            auctionHouseDao.create(auctionHouse);

            this.plugin.auctionHouseItems.buildAndAddMeta(auctionHouse.getId(), clonedItem,
                    player.getName(), price);

        } catch (SQLException e) {
            // ignore for now
        }

        itemInHand.setAmount(0);
        player.sendMessage(Component.text("Successfully placed an auction for your item", NamedTextColor.GREEN));

        return true;
    }
}
