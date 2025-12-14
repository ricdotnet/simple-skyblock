package dev.ricr.skyblock.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DatabaseChangesAccumulator {
    private final Queue<DatabaseChange> queue = new ConcurrentLinkedQueue<>();

    public void add(DatabaseChange changes) {
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
