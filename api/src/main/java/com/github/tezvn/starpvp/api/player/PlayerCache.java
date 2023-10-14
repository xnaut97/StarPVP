package com.github.tezvn.starpvp.api.player;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface PlayerCache {

    void queue(UUID uuid);

    boolean isQueued(UUID uuid);

    void remove(UUID uuid);

    /**
     * Queue player into cache list.
     */
    void queue(OfflinePlayer player);

    /**
     * Check if player is queued to cache
     */
    boolean isQueued(OfflinePlayer player);

    /**
     * Remove player from cache
     */
    void remove(OfflinePlayer player);

    /**
     * Force plugin to cache player data.
     * <br>Last cache time will not be affected by this method
     */
    void force();

    /**
     * Get last time plugin cache data.
     */
    long getLastCacheTime();

    /**
     * Get remaining time before plugin start to cache.
     */
    long getRemainingTime();

}
