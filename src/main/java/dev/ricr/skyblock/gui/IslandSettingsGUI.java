package dev.ricr.skyblock.gui;

import dev.ricr.skyblock.DisplayNames;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.enums.Buttons;
import dev.ricr.skyblock.permissions.IslandPermissions;
import dev.ricr.skyblock.permissions.Policies;
import dev.ricr.skyblock.utils.InventoryUtils;
import dev.ricr.skyblock.utils.ServerUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class IslandSettingsGUI implements InventoryHolder, ISimpleSkyblockGUI {
    private final SimpleSkyblock plugin;

    @Getter
    public final Inventory inventory;

    public IslandSettingsGUI(SimpleSkyblock plugin, Player player) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 45, Component.text(DisplayNames.ISLAND_SETTINGS));

        this.loadSettings(this.getIslandPermissions(player));
        InventoryUtils.fillEmptySlots(this.inventory);

        player.openInventory(this.inventory);
    }

    @Override
    public void handleInventoryClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        var clicked = event.getCurrentItem();

        if (clicked == null) {
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            this.handleClickGoBack(player);
            return;
        }

        String buttonType = clicked.getItemMeta()
                .getPersistentDataContainer()
                .get(ServerUtils.GUI_BUTTON_TYPE, PersistentDataType.STRING);
        Buttons button = Buttons.getByLabel(buttonType);

        var islandPermissions = this.getIslandPermissions(player);

        switch (button) {
            case null -> {
            }
            case Buttons.BreakBlocksButton -> islandPermissions.switchPermission(Policies.PoliciesEnum.BREAK_BLOCKS);
            case Buttons.PlaceBlocksButton -> islandPermissions.switchPermission(Policies.PoliciesEnum.PLACE_BLOCKS);
            case Buttons.KillMobsButton -> islandPermissions.switchPermission(Policies.PoliciesEnum.KILL_MOBS);
            case Buttons.InteractWithMobsButton -> islandPermissions.switchPermission(Policies.PoliciesEnum.INTERACT_WITH_MOBS);
            case Buttons.VillagerTradingButton-> islandPermissions.switchPermission(Policies.PoliciesEnum.VILLAGER_TRADING);
            case Buttons.PortalTravelButton -> islandPermissions.switchPermission(Policies.PoliciesEnum.PORTAL_TRAVEL);
            case Buttons.OpenInventoriesButton -> islandPermissions.switchPermission(Policies.PoliciesEnum.OPEN_INVENTORIES);
            case Buttons.OpenDoorsButton -> islandPermissions.switchPermission(Policies.PoliciesEnum.OPEN_DOORS);
            case Buttons.PvPButton -> islandPermissions.switchPermission(Policies.PoliciesEnum.PVP);
            case Buttons.IslandPrivacy,
                 Buttons.IslandAllowNetherTeleport,
                 Buttons.IslandAllowOfflineVisits,
                 Buttons.IslandAllowMobSpawning,
                 Buttons.IslandShowSeed,
                 Buttons.IslandTrustedPlayersList,
                 Buttons.IslandBlockedPlayersList,
                 Buttons.ModifyIslandSettings -> {/* ignore */}
        }

        this.loadSettings(islandPermissions);
        player.openInventory(this.inventory);
    }

    private void loadSettings(IslandPermissions islandPermissions) {
        addBooleanButton(
                islandPermissions.getPermissionValue(Policies.PoliciesEnum.BREAK_BLOCKS),
                10, Buttons.BreakBlocksButton, "ʙʀᴇᴀᴋ ʙʟᴏᴄᴋꜱ", "Allow other players to break blocks in your island."
        );
        addBooleanButton(
                islandPermissions.getPermissionValue(Policies.PoliciesEnum.PLACE_BLOCKS),
                11, Buttons.PlaceBlocksButton, "ᴘʟᴀᴄᴇ ʙʟᴏᴄᴋꜱ", "Allow other players to place blocks in your island."
        );
        addBooleanButton(
                islandPermissions.getPermissionValue(Policies.PoliciesEnum.KILL_MOBS),
                12, Buttons.KillMobsButton, "ᴋɪʟʟ ᴍᴏʙꜱ", "Allow other players to kill mobs in your island."
        );
        addBooleanButton(
                islandPermissions.getPermissionValue(Policies.PoliciesEnum.INTERACT_WITH_MOBS),
                13, Buttons.InteractWithMobsButton, "ɪɴᴛᴇʀᴀᴄᴛ ᴡɪᴛʜ ᴍᴏʙꜱ", "Allow other players to interact with mobs in your island."
        );
        addBooleanButton(
                islandPermissions.getPermissionValue(Policies.PoliciesEnum.VILLAGER_TRADING),
                14, Buttons.VillagerTradingButton, "ᴠɪʟʟᴀɢᴇʀ ᴛʀᴀᴅɪɴɢ", "Allow other players to trade with villagers in your island."
        );
        addBooleanButton(
                islandPermissions.getPermissionValue(Policies.PoliciesEnum.PORTAL_TRAVEL),
                15, Buttons.PortalTravelButton, "ᴘᴏʀᴛᴀʟ ᴛʀᴀᴠᴇʟ", "Allow other players to travel through your nether portal."
        );
        addBooleanButton(
                islandPermissions.getPermissionValue(Policies.PoliciesEnum.OPEN_INVENTORIES),
                16, Buttons.OpenInventoriesButton, "ᴏᴘᴇɴ ɪɴᴠᴇɴᴛᴏʀɪᴇꜱ", "Allow other players to use inventories in your island (chests, barrels, crafting table, etc)."
        );
        addBooleanButton(
                islandPermissions.getPermissionValue(Policies.PoliciesEnum.OPEN_DOORS),
                19, Buttons.OpenDoorsButton, "ᴏᴘᴇɴ ᴅᴏᴏʀꜱ", "Allow other players to open doors in your island."
        );
        addBooleanButton(
                islandPermissions.getPermissionValue(Policies.PoliciesEnum.PVP),
                20, Buttons.PvPButton, "ᴘᴠᴘ", "Allow PVP in your island."
        );

        ItemStack goBackButton = new ItemStack(Material.BARRIER, 1);
        ItemMeta meta = goBackButton.getItemMeta();
        meta.displayName(Component.text(DisplayNames.GO_BACK));
        goBackButton.setItemMeta(meta);
        this.inventory.setItem(40, goBackButton);
    }

    private void addBooleanButton(boolean isTrue, int inventoryPosition, Buttons buttonType, String label, String description) {
        ItemStack booleanButton;

        if (isTrue) {
            booleanButton = new ItemStack(Material.GREEN_TERRACOTTA);
            this.setItemComplexMeta(booleanButton, label, description, true, buttonType);
        } else {
            booleanButton = new ItemStack(Material.RED_TERRACOTTA);
            this.setItemComplexMeta(booleanButton, label, description, false, buttonType);
        }

        this.inventory.setItem(inventoryPosition, booleanButton);
    }

    private void setItemComplexMeta(ItemStack item, String label, String description, boolean state, Buttons buttonType) {
        var meta = item.getItemMeta();
        var itemMetaComponent = this.plugin.miniMessage
                .deserialize("<light_purple><label>", Placeholder.unparsed("label", label));

        var listOfLore = new ArrayList<Component>();
        listOfLore.addAll(
                List.of(Component.empty(),
                        Component.text("Enabled:", NamedTextColor.WHITE)
                                .appendSpace()
                                .append(Component.text(
                                        String.valueOf(state),
                                        state ? NamedTextColor.GREEN : NamedTextColor.RED
                                )),
                        Component.text("Click to change", NamedTextColor.GRAY),
                        Component.empty()));
        listOfLore.addAll(ServerUtils.wrapLore(description, 28, NamedTextColor.WHITE));

        meta.displayName(itemMetaComponent);
        meta.lore(listOfLore);

        meta.getPersistentDataContainer().set(
                ServerUtils.GUI_BUTTON_TYPE, PersistentDataType.STRING, buttonType.getLabel()
        );

        item.setItemMeta(meta);
    }

    private IslandPermissions getIslandPermissions(Player player) {
        var playerUniqueId = player.getUniqueId();
        return this.plugin.islandManager
                .getIslandRecord(playerUniqueId).islandPermissions();
    }

    private void handleClickGoBack(Player player) {
        this.inventory.close();
        player.openInventory(new IslandGUI(this.plugin, player).getInventory());
    }

}
