package com.example.blockdelay;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class PendingPlacementManager {
    private static final Map<UUID, PendingPlacement> PENDING_BY_PLAYER = new ConcurrentHashMap<>();
    private static final Set<UUID> BYPASS_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> RELEASE_REQUIRED_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, HeldInputState> USE_HELD_BY_PLAYER = new ConcurrentHashMap<>();

    private PendingPlacementManager() {
    }

    static PendingPlacement put(PendingPlacement placement) {
        return PENDING_BY_PLAYER.put(placement.playerId(), placement);
    }

    static PendingPlacement get(UUID playerId) {
        return PENDING_BY_PLAYER.get(playerId);
    }

    static PendingPlacement remove(UUID playerId) {
        return PENDING_BY_PLAYER.remove(playerId);
    }

    static Collection<PendingPlacement> all() {
        return PENDING_BY_PLAYER.values();
    }

    static void requireRelease(UUID playerId) {
        RELEASE_REQUIRED_PLAYERS.add(playerId);
    }

    static boolean isReleaseRequired(UUID playerId) {
        return RELEASE_REQUIRED_PLAYERS.contains(playerId);
    }

    static void clearReleaseRequired(UUID playerId) {
        RELEASE_REQUIRED_PLAYERS.remove(playerId);
    }

    static void updateUseHeld(UUID playerId, boolean held, long serverGameTime) {
        USE_HELD_BY_PLAYER.put(playerId, new HeldInputState(held, serverGameTime));
    }

    static HeldInputState getUseHeldState(UUID playerId) {
        return USE_HELD_BY_PLAYER.get(playerId);
    }

    static void clearUseHeld(UUID playerId) {
        USE_HELD_BY_PLAYER.remove(playerId);
    }

    static boolean isBypassing(UUID playerId) {
        return BYPASS_PLAYERS.contains(playerId);
    }

    static void beginBypass(UUID playerId) {
        BYPASS_PLAYERS.add(playerId);
    }

    static void endBypass(UUID playerId) {
        BYPASS_PLAYERS.remove(playerId);
    }

    record HeldInputState(boolean held, long lastUpdateGameTime) {
    }
}
