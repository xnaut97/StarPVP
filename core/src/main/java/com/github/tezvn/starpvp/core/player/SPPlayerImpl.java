package com.github.tezvn.starpvp.core.player;

import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.api.player.PlayerStatistic;
import com.github.tezvn.starpvp.api.rank.SPRank;
import com.github.tezvn.starpvp.core.SPPluginImpl;
import com.github.tezvn.starpvp.core.utils.MathUtils;
import com.google.common.collect.Maps;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Map;

public class SPPlayerImpl implements SPPlayer {

    private final OfflinePlayer player;

    private SPRank rank;

    private long starPoint;

    private long rankPoint;

    private final Map<PlayerStatistic, Long> statistic = Maps.newHashMap();

    public SPPlayerImpl(OfflinePlayer player) {
        this.player = player;
        this.rank = SPRank.COAL;
        this.rankPoint = getRank().getSP();
    }

    @Override
    public OfflinePlayer getPlayer() {
        return this.player;
    }

    @Override
    public long getStarPoint() {
        return this.rankPoint + this.starPoint;
    }

    @Override
    public void setStarPoint(long starPoint) {
        long offsetPoint = starPoint - this.starPoint;
        if(offsetPoint > 0) {
            double deathPercent = MathUtils.getPercent((int) getStatistic(PlayerStatistic.DEATH_COUNT),
                    (int) getStatistic(PlayerStatistic.TOTAL_COMBAT_TIMES));
            if(deathPercent >= getHellPercent())
                starPoint *= getHellMultiplier();
        }
        this.starPoint = starPoint;
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

    public void setStatistic(PlayerStatistic statistic, long value) {
        if (statistic == PlayerStatistic.TOTAL_COMBAT_TIMES)
            return;
        this.statistic.put(statistic, Math.max(0, value));
    }

    private double getHellPercent() {
        String str = JavaPlugin.getPlugin(SPPluginImpl.class).getConfig()
                .getString("hell-sp.activate", "60%");
        try {
            return MathUtils.roundDouble((double) Integer.parseInt(str) / 100);
        }catch (Exception e) {
            return 0;
        }
    }

    private double getHellMultiplier() {
        String str = JavaPlugin.getPlugin(SPPluginImpl.class).getConfig()
                .getString("hell-sp.multiplier", "35%");
        try {
            int value = Integer.parseInt(str);
            return MathUtils.roundDouble(1 - ((double) value /100), 2);
        }catch (Exception e) {
            return 1;
        }
    }

}
