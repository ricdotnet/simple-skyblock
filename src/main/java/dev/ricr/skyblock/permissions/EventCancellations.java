package dev.ricr.skyblock.permissions;

import dev.ricr.skyblock.enums.EventCancellationReasons;
import org.bukkit.event.Event;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public final class EventCancellations {

    private static final Map<Event, EventCancellationReasons> CANCELLATIONS = Collections.synchronizedMap(new IdentityHashMap<>());

    public static void add(Event event, EventCancellationReasons reason) {
        CANCELLATIONS.putIfAbsent(event, reason);
    }

    public static void replace(Event event, EventCancellationReasons reason) {
        CANCELLATIONS.put(event, reason);
    }

    public static @Nullable EventCancellationReasons get(Event event) {
        return CANCELLATIONS.get(event);
    }

    public static void remove(Event event) {
        CANCELLATIONS.remove(event);
    }
}
