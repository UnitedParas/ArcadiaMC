package net.arcadia.arcadiacore.sail;

import org.bukkit.Location;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SailStateManager {

    // Config knobs
    static final long CHANNEL_MS = 10_000;          // 10 seconds channel
    static final double BOOST_MULTIPLIER = 6.0;     // raise from 4 -> 6 (helps reach cap faster)
    static final double STILL_THRESH_SQ = 1e-4;     // squared distance threshold for "you moved"

    public enum Mode { IDLE, CHANNELING, BOOSTING }

    public static class SailState {
        public Mode mode = Mode.IDLE;

        // Channel data
        int channelTaskId = -1;
        long channelStartMs = 0L;
        Location channelStartLoc = null;
        UUID boatId = null;

        // Boost data
        boolean boosting = false;
        double originalMaxSpeed = -1;

        // Repeating task to push velocity every tick while boosting
        int boostTaskId = -1;
    }

    public final Map<UUID, SailState> map = new ConcurrentHashMap<>();

    public SailState get(UUID uuid) {
        return map.computeIfAbsent(uuid, u -> new SailState());
    }

    void clear(UUID uuid) {
        map.remove(uuid);
    }

    Collection<Map.Entry<UUID, SailState>> all() {
        return map.entrySet();
    }
}
