package dev.ricr.skyblock.database;

import dev.ricr.skyblock.SimpleSkyblock;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DatabaseChangesAccumulator {
    private final SimpleSkyblock plugin;
    private final Queue<DatabaseChange> queue = new ConcurrentLinkedQueue<>();

    public DatabaseChangesAccumulator(SimpleSkyblock plugin) {
        this.plugin = plugin;
    }

    public void add(DatabaseChange changes) {
        this.plugin.getLogger().info("Adding change to database queue: " + changes.toString());
        queue.add(changes);
    }

    public List<DatabaseChange> drain() {
        List<DatabaseChange> drained = new ArrayList<>();

        DatabaseChange change;
        while ((change = queue.poll()) != null) {
            drained.add(change);
        }

        return drained;
    }

}
