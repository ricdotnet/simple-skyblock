package dev.ricr.skyblock.shop;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.enums.SignShopType;
import dev.ricr.skyblock.utils.NumberUtils;
import dev.ricr.skyblock.utils.PlayerUtils;
import dev.ricr.skyblock.utils.ServerUtils;
import dev.ricr.skyblock.utils.Tuple;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

// left click is selling block or block to give or trade out
// right click is buying block or block to take or trade in

public class SignShop {
    private final SimpleSkyblock plugin;
    private PlayerInteractEvent playerInteractEvent = null;
    private SignChangeEvent signChangeEvent = null;
    private final Player player;
    private Material materialInHand = null;
    private Action actionType = null;
    private Sign sign = null;
    private SignShopType signShopType = null;

    public SignShop(SimpleSkyblock plugin, PlayerInteractEvent event, Sign sign, SignShopType signShopType) {
        this.plugin = plugin;

        this.playerInteractEvent = event;
        this.player = event.getPlayer();
        this.materialInHand = this.player.getInventory().getItemInMainHand().getType();
        this.actionType = event.getAction();

        this.sign = sign;
        this.signShopType = signShopType;

        if (this.signShopType == SignShopType.Trade) {
            this.handleSignTradeShop();
        } else if (this.signShopType == SignShopType.Shop) {
            // shop is still not available
        }
    }

    public SignShop(SimpleSkyblock plugin, SignChangeEvent event) {
        this.plugin = plugin;
        this.signChangeEvent = event;

        this.player = event.getPlayer();

        this.handleSignShopCreation();
    }

    private void handleSignTradeShop() {
        this.playerInteractEvent.setCancelled(true);

        if (this.isShopOwner(this.sign) && this.isShopActive(sign)) {
            this.player.sendMessage(Component.text("Your Sign Trade shop is already active", NamedTextColor.RED));
            return;
        }

        if (!this.isShopOwner(this.sign)) {
            if (!this.isShopActive(this.sign)) {
                this.player.sendMessage(Component.text("This sign shop is not active yet", NamedTextColor.RED));
                return;
            }

            this.handleSignShopTransaction();
            return;
        }

        if (this.signShopType == SignShopType.Trade) {
            if (this.actionType.isLeftClick()) {
                if (this.isOutItemSet(this.sign)) return;

                var shopOutItem = this.sign.getPersistentDataContainer()
                        .get(ServerUtils.SIGN_SHOP_OUT_ITEM, PersistentDataType.STRING);

                assert shopOutItem != null;
                var amount = shopOutItem.split(":")[1];
                shopOutItem = shopOutItem.replace("{item}", this.materialInHand.toString());

                this.sign.getPersistentDataContainer().set(
                        ServerUtils.SIGN_SHOP_OUT_ITEM,
                        PersistentDataType.STRING,
                        shopOutItem
                );

                var line = Component.text(String.format("%sx %s", amount, this.materialInHand), NamedTextColor.WHITE);

                this.sign.getSide(Side.FRONT).line(1, line);
                this.sign.update();

            } else if (this.actionType.isRightClick()) {
                if (this.isInItemSet(this.sign)) return;

                var shopInItem = this.sign.getPersistentDataContainer()
                        .get(ServerUtils.SIGN_SHOP_IN_ITEM, PersistentDataType.STRING);

                if (shopInItem == null) return;

                var amount = shopInItem.split(":")[1];
                shopInItem = shopInItem.replace("{item}", this.materialInHand.toString());

                this.sign.getPersistentDataContainer().set(
                        ServerUtils.SIGN_SHOP_IN_ITEM,
                        PersistentDataType.STRING,
                        shopInItem
                );

                var line = Component.text(String.format("%sx %s", amount, this.materialInHand), NamedTextColor.WHITE);

                this.sign.getSide(Side.FRONT).line(2, line);
                this.sign.update();
            }

            if (this.isShopActive(this.sign)) {
                this.player.sendMessage(Component.text("Your new Sign Trade shop is now active", NamedTextColor.GREEN));
            }
        }
    }

    private void handleSignShopCreation() {
        var world = this.player.getWorld();

        var signBlock = this.signChangeEvent.getBlock();
        var signBlockData = signBlock.getBlockData();
        var signBlockState = signBlock.getState();

        if (!(signBlockData instanceof WallSign wallSign) ||
                world.getName().equals("lobby") ||
                world.getEnvironment() == World.Environment.THE_END ||
                !PlayerUtils.isPlayerInOwnIsland(player, world.getName())
        ) {
            return;
        }

        if (this.getAttachedChestBlock(wallSign, signBlock) == null) {
            return;
        }

        // TODO: implement sign shop / trade
        var topLine = this.signChangeEvent.line(0);
        if (topLine == null) {
            return;
        }

        if (this.isShopActive((Sign) signBlockState)) {
            this.signChangeEvent.setCancelled(true);
            this.player.sendMessage(Component.text("This sign shop is already active and cannot be edited",
                    NamedTextColor.RED));
            return;
        }

        var topLineText = ServerUtils.getTextFromComponent(topLine);
        if (topLineText.equalsIgnoreCase("[trade]")) {
            this.makeTradeSign();
        }
    }

    private void makeTradeSign() {
        var line1 = this.signChangeEvent.line(1);
        var line2 = this.signChangeEvent.line(2);

        if (line1 == null || ServerUtils.getTextFromComponent(line1).isBlank()) {
            return;
        }

        var wallSign = (Sign) this.signChangeEvent.getBlock().getState();
        wallSign.getPersistentDataContainer().set(
                ServerUtils.SIGN_SHOP_TYPE,
                PersistentDataType.STRING,
                SignShopType.Trade.getLabel()
        );

        var outAmount = NumberUtils.objectToIntOrZero(ServerUtils.getTextFromComponent(line1));
        var inAmount = NumberUtils.objectToIntOrZero(ServerUtils.getTextFromComponent(line2));

        var topLine = Component.text(inAmount > 0 ? "[Trade]" : "[Free]")
                .style(Style.style(inAmount > 0 ? NamedTextColor.DARK_BLUE : NamedTextColor.GREEN, TextDecoration.BOLD, TextDecoration.ITALIC));
        this.signChangeEvent.line(0, topLine);

        var outAmountLine = Component.text("{trading_out}", NamedTextColor.WHITE);
        this.signChangeEvent.line(1, outAmountLine);
        wallSign.getPersistentDataContainer().set(
                ServerUtils.SIGN_SHOP_OUT_ITEM,
                PersistentDataType.STRING,
                String.format("{item}:%s", outAmount)
        );

        if (inAmount > 0) {
            var inAmountLine = Component.text("{trading_in}", NamedTextColor.WHITE);
            this.signChangeEvent.line(2, inAmountLine);
            wallSign.getPersistentDataContainer().set(
                    ServerUtils.SIGN_SHOP_IN_ITEM,
                    PersistentDataType.STRING,
                    String.format("{item}:%s", inAmount)
            );
        }

        Component shopMessage = Component.text("Left click to set selling block", NamedTextColor.GREEN);

        if (inAmount > 0) {
            shopMessage = shopMessage
                    .appendNewline()
                    .append(Component.text("Right click to set buying block", NamedTextColor.RED));
        }

        wallSign.getPersistentDataContainer().set(
                ServerUtils.SIGN_SHOP_OWNER,
                PersistentDataType.STRING,
                this.player.getUniqueId().toString()
        );

        wallSign.update();
        this.player.sendMessage(shopMessage);
    }

    private void handleSignShopTransaction() {
        var attachedBlock = this.getAttachedChestBlock((WallSign) this.sign.getBlockData(), this.sign.getBlock());
        if (attachedBlock == null || !(attachedBlock.getState() instanceof Chest chestBlock)) {
            return;
        }

        ItemStack itemToTradeIn = null;

        if (this.actionType.isRightClick()) {
            var itemToTradeOutTuple = this.getItemToTradeOut();
            var materialToTradeOut = itemToTradeOutTuple.getFirst();
            var amountToTradeOut = itemToTradeOutTuple.getSecond();

            if (!chestBlock.getInventory().contains(materialToTradeOut, amountToTradeOut)) {
                this.player.sendMessage(Component.text("There is not enough inventory", NamedTextColor.RED));
                return;
            }

            if (PlayerUtils.isInventoryFull(this.player) || !PlayerUtils.hasSpaceInInventory(this.player, amountToTradeOut)) {
                this.player.sendMessage(Component.text("You have no space in your inventory", NamedTextColor.RED));
                return;
            }

            var itemToTradeInTuple = this.getItemToTradeIn();
            if (itemToTradeInTuple != null) {
                var materialToTradeIn = itemToTradeInTuple.getFirst();
                var amountToTradeIn = itemToTradeInTuple.getSecond();

                if (this.player.getInventory().contains(materialToTradeIn, amountToTradeIn)) {
                    itemToTradeIn = new ItemStack(materialToTradeIn, amountToTradeIn);
                    this.player.getInventory().removeItem(itemToTradeIn);
                } else {
                    this.player.sendMessage(Component.text("You do not have enough items to trade in", NamedTextColor.RED));
                    return;
                }
            }

            var itemToTradeOut = new ItemStack(materialToTradeOut, amountToTradeOut);
            chestBlock.getInventory().removeItem(itemToTradeOut);
            this.player.getInventory().addItem(itemToTradeOut);

            if (itemToTradeIn != null) {
                chestBlock.getInventory().addItem(itemToTradeIn);
            }

            this.player.sendMessage(Component.text("You have successfully traded out your items", NamedTextColor.GREEN));
        }
    }

    private Tuple<Material, Integer> getItemToTradeOut() {
        var signShopOut = sign.getPersistentDataContainer().get(ServerUtils.SIGN_SHOP_OUT_ITEM, PersistentDataType.STRING);
        assert signShopOut != null;

        var materialToTradeOut = signShopOut.split(":")[0];
        var amountToTradeOut = signShopOut.split(":")[1];

        return new Tuple<>(Material.getMaterial(materialToTradeOut), Integer.parseInt(amountToTradeOut));
    }

    private Tuple<Material, Integer> getItemToTradeIn() {
        var signShopIn = sign.getPersistentDataContainer().get(ServerUtils.SIGN_SHOP_IN_ITEM, PersistentDataType.STRING);
        if (signShopIn == null) return null;

        var materialToTradeIn = signShopIn.split(":")[0];
        var amountToTradeIn = signShopIn.split(":")[1];

        return new Tuple<>(Material.getMaterial(materialToTradeIn), Integer.parseInt(amountToTradeIn));
    }

    private boolean isOutItemSet(Sign sign) {
        var signShopOut = sign.getPersistentDataContainer().get(ServerUtils.SIGN_SHOP_OUT_ITEM, PersistentDataType.STRING);

        return signShopOut != null && !signShopOut.contains("{item}");
    }

    private boolean isInItemSet(Sign sign) {
        var signShopIn = sign.getPersistentDataContainer().get(ServerUtils.SIGN_SHOP_IN_ITEM, PersistentDataType.STRING);

        return signShopIn == null || !signShopIn.contains("{item}");
    }

    private boolean isShopActive(Sign sign) {
        return this.isInItemSet(sign) && this.isOutItemSet(sign);
    }

    private boolean isShopOwner(Sign sign) {
        var shopOwnerId = sign.getPersistentDataContainer().get(ServerUtils.SIGN_SHOP_OWNER, PersistentDataType.STRING);
        return shopOwnerId != null && shopOwnerId.equals(this.player.getUniqueId().toString());
    }

    private Block getAttachedChestBlock(WallSign wallSign, Block signBlock) {
        var facing = wallSign.getFacing();
        var attachedBlock = signBlock.getRelative(facing.getOppositeFace());

        return attachedBlock.getType() == Material.CHEST ? attachedBlock : null;
    }
}
