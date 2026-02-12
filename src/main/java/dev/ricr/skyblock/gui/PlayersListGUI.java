package dev.ricr.skyblock.gui;

import dev.ricr.skyblock.database.PlayerEntity;
import dev.ricr.skyblock.utils.InventoryUtils;
import dev.ricr.skyblock.utils.PlayerUtils;
import dev.ricr.skyblock.utils.Tuple;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayersListGUI<T> implements InventoryHolder, ISimpleSkyblockGUI {
    @Getter
    private final Inventory inventory;
    private final List<Tuple<UUID, String>> playerTuples = new ArrayList<>();

    public PlayersListGUI(Player player, List<T> playerList, String title) {
        this.inventory = Bukkit.createInventory(this, 27, Component.text(title + " (" + playerList.size() + ")"));

        if (!playerList.isEmpty()) {
            switch (playerList.getFirst()) {
                case Player ignore -> this.getPlayersPlayer((List<Player>) playerList);
                case PlayerEntity ignore -> this.getPlayersPlayerEntity((List<PlayerEntity>) playerList);
                case Tuple<?, ?> ignore -> this.copyPlayerTuples((List<Tuple<String, String>>) playerList);
                default -> {/* we ignore other types */}
            }
        }

        for (int i = 0; i < playerTuples.size(); i++) {
            var playerTuple = playerTuples.get(i);
            var playerHead = PlayerUtils.getPlayerHead(playerTuple.getFirst(), playerTuple.getSecond());
            this.inventory.setItem(i, playerHead);
        }

        InventoryUtils.fillEmptySlots(this.inventory);
        player.openInventory(this.inventory);
    }

    private void getPlayersPlayer(List<Player> players) {
        for (var player : players) {
            this.playerTuples.add(new Tuple<>(player.getUniqueId(), player.getName()));
        }
    }

    private void getPlayersPlayerEntity(List<PlayerEntity> playerEntities) {
        for (var playerEntity : playerEntities) {
            this.playerTuples.add(new Tuple<>(UUID.fromString(playerEntity.getPlayerId()), playerEntity.getUsername()));
        }
    }

    private void copyPlayerTuples(List<Tuple<String, String>> playerTuples) {
        for (var playerTuple : playerTuples) {
            this.playerTuples.add(new Tuple<>(UUID.fromString(playerTuple.getFirst()), playerTuple.getSecond()));
        }
    }

    @Override
    public void handleInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
