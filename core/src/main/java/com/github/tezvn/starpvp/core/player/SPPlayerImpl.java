package com.github.tezvn.starpvp.core.player;

import com.github.tezvn.starpvp.api.player.PlayerStatistic;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.api.rank.SPRank;
import com.github.tezvn.starpvp.core.SPPluginImpl;
import com.github.tezvn.starpvp.core.utils.MathUtils;
import com.google.common.collect.Maps;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;

public class SPPlayerImpl implements SPPlayer {

    private final UUID uniqueId;

    private final String playerName;

    private SPRank rank;

    private long starPoint;

    private long rankPoint;

    private int penaltyTimes;

    private boolean isPenalty;

    private final Map<PlayerStatistic, Long> statistic = Maps.newHashMap();

    public SPPlayerImpl(OfflinePlayer player) {
        this.uniqueId = player.getUniqueId();
        this.playerName = player.getName();
        this.rank = SPRank.COAL;
        this.rankPoint = getRank().getSP();
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
    public OfflinePlayer getPlayer() {
        return Bukkit.getOfflinePlayer(this.getUniqueId());
    }

    @Override
    public long getStarPoint() {
        return this.rankPoint + this.starPoint;
    }

    @Override
    public void setStarPoint(long starPoint) {
        long offsetPoint = starPoint - this.starPoint;
        if(offsetPoint > 0) {
            long deathCount = getStatistic(PlayerStatistic.DEATH_COUNT);
            long killCount = getStatistic(PlayerStatistic.KILL_COUNT);
            if(deathCount > killCount) {
                double deathPercent = MathUtils.getPercent((int) getStatistic(PlayerStatistic.DEATH_COUNT),
                        (int) getStatistic(PlayerStatistic.TOTAL_COMBAT_TIMES));
                if (deathPercent >= getPercent("hell-sp.activate"))
                    offsetPoint *= getMultiplier("hell-sp.multiplier");
            }else {
                double killPercent = MathUtils.getPercent((int) getStatistic(PlayerStatistic.KILL_COUNT),
                        (int) getStatistic(PlayerStatistic.TOTAL_COMBAT_TIMES));
                if(killPercent >= getPercent("high-sp.activate"))
                    offsetPoint += (offsetPoint*getMultiplier("high-sp.multiplier"));
            }
        }
        this.starPoint += offsetPoint;
        if (this.starPoint > 0) {
            long offset = this.rank.getNext().getSP() - this.rank.getSP();
            if (this.starPoint - offset >= 0) {
                this.rank = rank.getNext();
                this.rankPoint = this.rank.getSP();
                this.starPoint = offset - this.starPoint;
                setStarPoint(offset - this.starPoint);
            }
        } else {
            long offset = this.rank.getSP() - this.rank.getPrevious().getSP();
            this.rank = rank.getPrevious();
            this.rankPoint = this.rank.getSP();
            setStarPoint(offset + starPoint);
        }
    }

    @Override
    public SPRank getRank() {
        return rank;
    }

    @Override
    public void setRank(SPRank rank, boolean resetSP) {
        this.rank = rank;
        this.rankPoint = rank.getSP();
        setStarPoint(resetSP ? 0 : getStarPoint());
    }

    @Override
    public void setRank(SPRank rank) {
        setRank(rank, false);
    }

    @Override
    public long getStatistic(PlayerStatistic statistic) {
        return this.statistic.getOrDefault(statistic, 0L);
    }

    @Override
    public void setStatistic(PlayerStatistic statistic, long value) {
        if (statistic == PlayerStatistic.TOTAL_COMBAT_TIMES)
            return;
        this.statistic.put(statistic, Math.max(0, value));
    }

    @Override
    public long getPenaltyTimes() {
        return this.penaltyTimes;
    }

    @Override
    public boolean isPenalty() {
        return isPenalty;
    }

    public void enterCombatLogout() {
        if(this.isPenalty())
            return;
        this.penaltyTimes +=1;
    }

    public void leaveCombatLogOut() {
        this.isPenalty = false;
    }

    private double getPercent(String key) {
        String str = JavaPlugin.getPlugin(SPPluginImpl.class).getDocument()
                .getString(key, "60%");
        try {
            return MathUtils.roundDouble((double) Integer.parseInt(str) / 100);
        }catch (Exception e) {
            return 0;
        }
    }

    private double getMultiplier(String key) {
        String str = JavaPlugin.getPlugin(SPPluginImpl.class).getDocument()
                .getString(key, "35%");
        try {
            int value = Integer.parseInt(str);
            return MathUtils.roundDouble(1 - ((double) value /100), 2);
        }catch (Exception e) {
            return 1;
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = Maps.newHashMap();
        map.put("uuid", getUniqueId().toString());
        map.put("rank", getRank().name());
        map.put("sp.total", String.valueOf(this.getStarPoint()));
        map.put("sp.current", String.valueOf(this.starPoint));
        map.put("penalty.times", String.valueOf(getPenaltyTimes()));
        map.put("penalty.activate", String.valueOf(this.isPenalty()));
        return map;
    }
}
