package com.github.tezvn.starpvp.core.player;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.cooldown.Cooldown;
import com.github.tezvn.starpvp.api.player.PlayerStatistic;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.api.rank.RankManager;
import com.github.tezvn.starpvp.api.rank.SPRank;
import com.github.tezvn.starpvp.core.SPPluginImpl;
import com.github.tezvn.starpvp.core.utils.MathUtils;
import com.github.tezvn.starpvp.core.utils.time.TimeUtils;
import com.google.common.collect.Maps;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class SPPlayerImpl implements SPPlayer {

    private final UUID uniqueId;

    private final String playerName;

    private SPRank rank;

    private long eloPoint;

    private final Map<PlayerStatistic, Long> statistic = Maps.newHashMap();

    private Cooldown cooldown;

    private final Map<UUID, Long> kills = Maps.newHashMap();

    public SPPlayerImpl(OfflinePlayer player) {
        this.uniqueId = player.getUniqueId();
        this.playerName = player.getName();
    }

    @Override
    public UUID getUniqueId() {
        return uniqueId;
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }

    @Override
    public OfflinePlayer asOfflinePlayer() {
        return Bukkit.getOfflinePlayer(this.getUniqueId());
    }

    @Override
    public Player asPlayer() {
        return asOfflinePlayer().getPlayer();
    }

    @Override
    public long getEloPoint() {
        return this.eloPoint;
    }

    @Override
    public void setEloPoint(long eloPoint) {
        this.eloPoint = Math.max(0, eloPoint);
        updateRank();
    }

    @Override
    public void addEloPoint(long eloPoint) {
        this.eloPoint += Math.abs(eloPoint);
        updateRank();
    }

    @Override
    public void subtractEloPoint(long eloPoint) {
        this.eloPoint -= Math.abs(eloPoint);
        updateRank();
    }

    private void updateRank() {
        RankManager rankManager = JavaPlugin.getPlugin(SPPluginImpl.class).getRankManager();
        List<SPRank> sorted = rankManager.getRanks().stream().sorted(Comparator.comparing(
                SPRank::getElo, Comparator.naturalOrder())).toList();
        int order = 0;
        for (int i = 0; i < sorted.size(); i++) {
            SPRank rank = sorted.get(i);
            if (rank.getElo() > getEloPoint()) break;
            order = i;
        }
        this.rank = sorted.get(order);
    }

    @Override
    public SPRank getRank() {
        return rank;
    }

    @Override
    public long getStatistic(PlayerStatistic statistic) {
        return this.statistic.getOrDefault(statistic, 0L);
    }

    @Override
    public void setStatistic(PlayerStatistic statistic, long value) {
        this.statistic.put(statistic, Math.max(0, value));
    }

    @Override
    public <T extends Cooldown> T getCooldown() {
        return (T) cooldown;
    }

    @Override
    public <T extends Cooldown> void setCooldown(T cooldown) {
        this.cooldown = cooldown;
    }

    @Override
    public void removeCooldown() {
        this.cooldown = null;
    }

    @Override
    public Map<UUID, Long> getKillsCooldown() {
        return this.kills;
    }

    @Override
    public long getKillCooldown(OfflinePlayer player) {
        return this.kills.getOrDefault(player.getUniqueId(), 0L);
    }

    @Override
    public void addKillCooldown(OfflinePlayer player) {
        SPPlugin plugin = JavaPlugin.getPlugin(SPPluginImpl.class);
        String duration = plugin.getDocument().getString("cooldown.kill-other", "5m");
        this.kills.putIfAbsent(player.getUniqueId(), TimeUtils.newInstance().add(duration).getNewTime());
    }

    public void addKillCooldown(UUID uuid, long time) {
        this.kills.putIfAbsent(uuid, time);
    }

    public void removeKillCooldown(OfflinePlayer player) {
        this.kills.remove(player.getUniqueId());
    }

    private double getPercent(String key) {
        String str = JavaPlugin.getPlugin(SPPluginImpl.class).getDocument()
                .getString(key, "60%");
        try {
            return MathUtils.roundDouble((double) Integer.parseInt(str) / 100);
        } catch (Exception e) {
            return 0;
        }
    }

    private double getMultiplier(String key) {
        String str = JavaPlugin.getPlugin(SPPluginImpl.class).getDocument()
                .getString(key, "35%");
        try {
            int value = Integer.parseInt(str);
            return MathUtils.roundDouble(1 - ((double) value / 100), 2);
        } catch (Exception e) {
            return 1;
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = Maps.newHashMap();
        map.put("uuid", getUniqueId().toString());
        map.put("name", getPlayerName());
        map.put("rank", getRank().getId());
        map.put("elo", String.valueOf(this.getEloPoint()));
        statistic.forEach((type, value) -> map.put("statistic." + type.name(), value));
        if (getCooldown() != null) {
            map.put("cooldown.type", getCooldown().getType().name());
            map.put("cooldown.start", getCooldown().getStartTime());
            map.put("cooldown.end", getCooldown().getEndTime());
        }
        kills.forEach((uuid, value) -> map.put("kills-cooldown." + uuid.toString(), value));
        return map;
    }

}
