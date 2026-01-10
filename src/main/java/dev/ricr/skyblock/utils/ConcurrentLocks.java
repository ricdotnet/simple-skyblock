package dev.ricr.skyblock.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public final class ConcurrentLocks {
    private static final ConcurrentHashMap<Object, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

    public static ReentrantLock getLock(Object objectId) {
        return LOCKS.computeIfAbsent(objectId, id -> new ReentrantLock());
    }

    public static void removeLock(Object objectId) {
        LOCKS.remove(objectId);
    }
}
