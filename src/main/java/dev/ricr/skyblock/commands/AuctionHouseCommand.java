package dev.ricr.skyblock.commands;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.AuctionHouseItemEntity;
import dev.ricr.skyblock.database.DatabaseChange;
import dev.ricr.skyblock.gui.AuctionHouseGUI;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

@AllArgsConstructor
public class AuctionHouseCommand implements CommandExecutor {
    private final SimpleSkyblock plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             String[] args) {
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

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
            var auctionHouseGUI = new AuctionHouseGUI(this.plugin);
            player.openInventory(auctionHouseGUI.getInventory());

            return true;
        }

        double price = Double.parseDouble(args[0]);

        if (price <= 0) {
            player.sendMessage(Component.text("You cannot place an auction for a negative or zero price",
                    NamedTextColor.RED));

            return true;
        }

        var itemInHand = player.getInventory().getItemInMainHand();
        var clonedItem = itemInHand.clone();

        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage(Component.text("You must be holding an item to place an auction for it",
                    NamedTextColor.RED));
            return true;
        }

        var playerSelling = this.plugin.onlinePlayers.getPlayer(player.getUniqueId());

        var playerListingsCount = 0L;
        try {
            playerListingsCount = this.plugin.databaseManager.getAuctionHouseDao().queryBuilder()
                    .where()
                    .eq("player_id", playerSelling.getPlayerId())
                    .countOf();
        } catch (SQLException e) {
            // ignore for now
        }

        if (playerListingsCount >= ServerUtils.AUCTION_HOUSE_MAX_LISTINGS) {
            player.sendMessage(Component.text("You cannot place more than 10 auctions",
                    NamedTextColor.RED));
            return true;
        }

        var auctionHouseItemEntity = new AuctionHouseItemEntity();
        auctionHouseItemEntity.setPlayer(playerSelling);
        auctionHouseItemEntity.setOwnerName(player.getName());
        auctionHouseItemEntity.setPrice(price);
        auctionHouseItemEntity.setItem(ServerUtils.base64FromBytes(itemInHand.serializeAsBytes()));

        var auctionHouseAdd = new DatabaseChange.AuctionHouseItemAdd(auctionHouseItemEntity);
        this.plugin.databaseChangesAccumulator.add(auctionHouseAdd);

        this.plugin.auctionHouseItems.buildAndAddMeta(auctionHouseItemEntity.getId(), clonedItem, player.getName(), price);

        itemInHand.setAmount(0);
        player.sendMessage(Component.text("Successfully placed an auction for your item", NamedTextColor.GREEN));

        return true;
    }
}
