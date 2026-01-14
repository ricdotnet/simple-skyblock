package dev.ricr.skyblock.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.AuctionHouseItemEntity;
import dev.ricr.skyblock.database.DatabaseChange;
import dev.ricr.skyblock.gui.AuctionHouseGUI;
import dev.ricr.skyblock.utils.ServerUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.AllArgsConstructor;
import org.bukkit.Material;

import java.sql.SQLException;
import java.util.List;

@AllArgsConstructor
public class AuctionHouseCommand implements ICommand {
    private final SimpleSkyblock plugin;

    public void register() {
        this.plugin.getLifecycleManager()
                .registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
                    LiteralCommandNode<CommandSourceStack> auctionHouse = this.command();
                    commands.registrar().register(auctionHouse, List.of("ah"));
                });
    }

    private LiteralCommandNode<CommandSourceStack> command() {
        return Commands.literal("auctionhouse")
                .executes(this::openAuctionHouseGUI)
                .then(Commands.literal("sell")
                        .then(Commands.argument("price", DoubleArgumentType.doubleArg())
                                .executes(this::sellItem)
                        )
                )
                .build();
    }

    private int openAuctionHouseGUI(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var auctionHouseGUI = new AuctionHouseGUI(this.plugin);
        player.openInventory(auctionHouseGUI.getInventory());

        return Command.SINGLE_SUCCESS;
    }


    private int sellItem(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var price = ctx.getArgument("price", Double.class);

        if (price <= 0) {
            player.sendMessage(this.plugin.miniMessage.deserialize("<red>You cannot place an auction for a negative or zero price"));
            return Command.SINGLE_SUCCESS;
        }

        var itemInHand = player.getInventory().getItemInMainHand();
        var clonedItem = itemInHand.clone();

        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage(this.plugin.miniMessage.deserialize("<red>You must be holding an item to place an auction for it"));
            return Command.SINGLE_SUCCESS;
        }

        var playerSellingEntity = this.plugin.onlinePlayers.getPlayer(player.getUniqueId()).getPlayerEntity();

        var playerListingsCount = 0L;
        try {
            playerListingsCount = this.plugin.databaseManager.getAuctionHouseDao().queryBuilder()
                    .where()
                    .eq("player_id", playerSellingEntity.getPlayerId())
                    .countOf();
        } catch (SQLException e) {
            // ignore for now
        }

        if (playerListingsCount >= ServerUtils.AUCTION_HOUSE_MAX_LISTINGS) {
            player.sendMessage(this.plugin.miniMessage.deserialize("<red>You cannot place more than 10 auctions"));
            return Command.SINGLE_SUCCESS;
        }

        var auctionHouseItemEntity = new AuctionHouseItemEntity();
        auctionHouseItemEntity.setPlayer(playerSellingEntity);
        auctionHouseItemEntity.setOwnerName(player.getName());
        auctionHouseItemEntity.setPrice(price);
        auctionHouseItemEntity.setItem(ServerUtils.base64FromBytes(itemInHand.serializeAsBytes()));

        var auctionHouseAdd = new DatabaseChange.AuctionHouseItemAdd(auctionHouseItemEntity);
        this.plugin.databaseChangesAccumulator.add(auctionHouseAdd);

        this.plugin.auctionHouseItems.buildAndAddMeta(auctionHouseItemEntity.getId(), clonedItem, player.getName(), price);

        itemInHand.setAmount(0);
        player.sendMessage(this.plugin.miniMessage.deserialize("<green>Successfully placed an auction for your item"));

        return Command.SINGLE_SUCCESS;
    }
}
