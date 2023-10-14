package com.github.tezvn.starpvp.core.player;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.PlayerCache;
import com.github.tezvn.starpvp.api.player.PlayerManager;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.core.utils.time.TimeUtils;
import com.google.common.collect.Lists;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultPlayerCache extends BukkitRunnable implements PlayerCache {

    private final SPPlugin plugin;

    private final PlayerManager playerManager;

    private final ExecutorService pool = Executors.newFixedThreadPool(10);

    private final List<UUID> cachePlayers = Lists.newArrayList();

    private long lastCache;

    public DefaultPlayerCache(SPPlugin plugin) {
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerManager();
        long period = TimeUtils.newInstance().add(getPeriod()).getDuration()/1000*20;
        this.runTaskTimerAsynchronously(plugin, period, period);
        this.lastCache = TimeUtils.newInstance().getNewTime();
    }

    @Override
    public void run() {
        cache();
        this.lastCache = TimeUtils.newInstance().getNewTime();
    }

    @Override
    public void queue(UUID uuid) {
        if(isQueued(uuid)) return;
        this.cachePlayers.add(uuid);
    }

    @Override
    public boolean isQueued(UUID uuid) {
        return cachePlayers.contains(uuid);
    }

    @Override
    public void remove(UUID uuid) {
        this.cachePlayers.removeIf(u -> u.equals(uuid));
    }

    @Override
    public void queue(OfflinePlayer player) {
        queue(player.getUniqueId());
    }

    @Override
    public boolean isQueued(OfflinePlayer player) {
        return isQueued(player.getUniqueId());
    }

    @Override
    public void remove(OfflinePlayer player) {
        remove(player.getUniqueId());
    }

    @Override
    public void force() {
        cache();
    }

    @Override
    public long getLastCacheTime() {
        return this.lastCache;
    }

    @Override
    public long getRemainingTime() {
        long nextCache = TimeUtils.of(lastCache).add(getPeriod()).getNewTime();
        long duration = nextCache - TimeUtils.newInstance().getNewTime();
        return Math.max(0, duration);
    }

    private void cache() {
        if(cachePlayers.isEmpty()) return;
        cachePlayers.forEach(u -> pool.submit(() -> playerManager.saveToDatabase(u)));
        plugin.getLogger().info("Cached " + cachePlayers.size() + " players data to database!");
        this.cachePlayers.clear();
    }

    private String getPeriod() {
        String period;
        if(plugin.getDocument() == null) period = "5m";
        else period = plugin.getDocument().getOptionalString("auto-cache").orElse("5m");
        return period;
    }
}
