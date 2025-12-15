package dev.ricr.skyblock.listeners;

import com.j256.ormlite.dao.Dao;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.DatabaseChange;
import dev.ricr.skyblock.database.IslandEntity;
import dev.ricr.skyblock.database.PlayerEntity;
import dev.ricr.skyblock.enums.SignShopType;
import dev.ricr.skyblock.utils.NumberUtils;
import dev.ricr.skyblock.utils.PlayerUtils;
import dev.ricr.skyblock.utils.ServerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;

public class PlayerListeners implements Listener {
    private final SimpleSkyblock plugin;

    public PlayerListeners(SimpleSkyblock plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();

        var isUsingLobbyWorld = this.plugin.serverConfig.getBoolean("lobby", true);
        if (!isUsingLobbyWorld) {
            // TODO: implement island creation on join
            return;
        }

        var lobbyWorld = ServerUtils.loadOrCreateLobby();
        player.sendMessage(Component.text("Welcome to SimpleSkyblock!", NamedTextColor.GREEN));

        this.createPlayerEntity(player);
        this.plugin.islandManager.addPlayerIsland(player.getUniqueId());

        // always start in the lobby / spawn world
        player.teleport(new Location(lobbyWorld, 0.5, 65, 0.5));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();

        this.plugin.onlinePlayers.removePlayer(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (!event.isBedSpawn()) {
            this.plugin.getLogger()
                    .info(String.format("Player %s does not have a bed, sending to lobby", player.getName()));

            // TODO: if player has no island teleport to lobby then
            var lobbyWorld = ServerUtils.loadOrCreateLobby();
            event.setRespawnLocation(new Location(lobbyWorld, 0.5, 65, 0.5));
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        var player = event.getPlayer();
        var world = player.getWorld();

        if (world.getName().equals("lobby") || world.getEnvironment() == World.Environment.THE_END) {
            return;
        }

        IslandEntity playerIsland = null;
        var islandId = world.getName()
                .replace("islands/", "")
                .replace("nether_", "");
        try {
            playerIsland = this.plugin.databaseManager.getIslandsDao().queryForId(islandId);
        } catch (SQLException e) {
            // ignore for now
        }

        if (playerIsland == null) {
            return;
        }

        var message = Component.text("-=", NamedTextColor.GREEN)
                .appendSpace()
                .append(Component.text(String.format("%s's island", playerIsland.getPlayer().getUsername()), NamedTextColor.GOLD))
                .appendSpace()
                .append(Component.text("=-", NamedTextColor.GREEN));

        PlayerUtils.showTitleMessage(this.plugin, player, message, 20L);
    }

    @EventHandler
    public void onSignChangeEvent(SignChangeEvent event) {
        var player = event.getPlayer();
        var world = player.getWorld();

        var signBlock = event.getBlock();
        var signBlockData = signBlock.getBlockData();

        if (!(signBlockData instanceof WallSign wallSign) ||
                world.getName().equals("lobby") ||
                world.getEnvironment() == World.Environment.THE_END ||
                !PlayerUtils.isPlayerInOwnIsland(player, world.getName())
        ) {
            return;
        }

        var facing = wallSign.getFacing();
        var attachedBlock = signBlock.getRelative(facing.getOppositeFace());

        if (attachedBlock.getType() != Material.CHEST) {
            return;
        }

        // TODO: implement sign shop / trade
        var topLine = event.line(0);
        if (topLine == null) {
            return;
        }

        var topLineText = ServerUtils.getTextFromComponent(topLine);
        if (topLineText.equalsIgnoreCase("[trade]")) {
            this.makeTradeSign(event);
        }
    }

    private void createPlayerEntity(Player player) {
        Dao<PlayerEntity, String> playersDao = this.plugin.databaseManager.getPlayersDao();

        var playerUniqueId = player.getUniqueId();

        try {
            var playerEntity = playersDao.queryForId(playerUniqueId.toString());

            if (playerEntity != null) {
                this.plugin.getLogger()
                        .info(String.format("Player %s already joined before. Skipping initialization of player entity.",
                                player.getName()));

                this.plugin.onlinePlayers.addPlayer(playerUniqueId, playerEntity);
                return;
            }

            playerEntity = new PlayerEntity();
            playerEntity.setPlayerId(playerUniqueId.toString());
            playerEntity.setUsername(player.getName());
            playerEntity.setBalance(100.0d);

            this.plugin.onlinePlayers.addPlayer(playerUniqueId, playerEntity);

            var playerCreateOrUpdate = new DatabaseChange.PlayerCreateOrUpdate(playerEntity);
            this.plugin.databaseChangesAccumulator.add(playerCreateOrUpdate);
        } catch (SQLException e) {
            // ignore for now
        }
    }

    private void makeTradeSign(SignChangeEvent event) {
        var line1 = event.line(1);
        var line2 = event.line(2);

        if (line1 == null || ServerUtils.getTextFromComponent(line1).isBlank()) {
            return;
        }

        var wallSign = (Sign) event.getBlock().getState();
        wallSign.getPersistentDataContainer().set(
                ServerUtils.SIGN_SHOP_TYPE,
                PersistentDataType.STRING,
                SignShopType.Trade.getLabel()
        );

        var outAmount = NumberUtils.objectToIntOrZero(ServerUtils.getTextFromComponent(line1));
        var inAmount = NumberUtils.objectToIntOrZero(ServerUtils.getTextFromComponent(line2));

        var topLine = Component.text(inAmount > 0 ? "[Trade]" : "[Free]")
                .style(Style.style(inAmount > 0 ? NamedTextColor.DARK_BLUE : NamedTextColor.GREEN, TextDecoration.BOLD, TextDecoration.ITALIC));
        event.line(0, topLine);

        var outAmountLine = Component.text("{trading_out}", NamedTextColor.WHITE);
        event.line(1, outAmountLine);
        wallSign.getPersistentDataContainer().set(
                ServerUtils.SIGN_SHOP_OUT_ITEM,
                PersistentDataType.STRING,
                String.format("{item}:%s", outAmount)
        );

        if (inAmount > 0) {
            var inAmountLine = Component.text("{trading_in}", NamedTextColor.WHITE);
            event.line(2, inAmountLine);
            wallSign.getPersistentDataContainer().set(
                    ServerUtils.SIGN_SHOP_IN_ITEM,
                    PersistentDataType.STRING,
                    String.format("{item}:%s", inAmount)
            );
        }

        var player = event.getPlayer();
        Component shopMessage = Component.text("Left click to set selling block", NamedTextColor.GREEN);

        if (inAmount > 0) {
            shopMessage = shopMessage
                    .appendNewline()
                    .append(Component.text("Right click to set buying block", NamedTextColor.RED));
        }

        wallSign.getPersistentDataContainer().set(
                ServerUtils.SIGN_SHOP_OWNER,
                PersistentDataType.STRING,
                player.getUniqueId().toString()
        );

        wallSign.update();
        player.sendMessage(shopMessage);
    }
}

