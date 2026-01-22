package dev.ricr.skyblock.utils;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyChestManager {
    @Getter
    private final Map<String, KeyChest> keyChests;

    public KeyChestManager() {
        this.keyChests = new HashMap<>();
    }

    public void addKeyChest(KeyChest keyChest) {
        this.keyChests.put(keyChest.name(), keyChest);
    }

    public KeyChest getKeyChest(String name) {
        return this.keyChests.get(name);
    }

    public record KeyChest(String name, String displayName, List<ItemStack> items) {}
}
