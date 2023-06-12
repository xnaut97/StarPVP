package com.github.tezvn.starpvp.core.player;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.PlayerStatistic;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.api.player.RankTier;
import com.github.tezvn.starpvp.api.player.SPRank;
import com.github.tezvn.starpvp.core.SPPluginImpl;
import com.google.common.collect.Maps;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class SPPlayerImpl implements SPPlayer {

    private final OfflinePlayer player;

    private long starPoint;

    private SPRank rank = new SPRankImpl(this);

    private final Map<PlayerStatistic, Long> statistic = Maps.newHashMap();

    public SPPlayerImpl(OfflinePlayer player) {
        this.player = player;
    }

    @Override
    public OfflinePlayer getPlayer() {
        return this.player;
    }

    @Override
    public long getStarPoint() {
        return this.starPoint;
    }

    @Override
    public void setStarPoint(long starPoint) {
        SPPlugin plugin = JavaPlugin.getPlugin(SPPluginImpl.class);
        FileConfiguration config = plugin.getConfig();
        long maxLevel = config.getLong("max-level");
        long spToUpgrade = config.getLong("sp-to-upgrade");
        if(starPoint - spToUpgrade > 0) {
            this.setRank(getRank().getTier(), getRank().getLevel()+1);
            if(this.getRank().getLevel() >= maxLevel)
                this.rank = this.rank.getNext();
            setStarPoint(starPoint - spToUpgrade);
            return;
        }
        this.starPoint = starPoint;
    }

    @Override
    public SPRank getRank() {
        return rank;
    }

    @Override
    public void setRank(RankTier rank, int level) {
        this.rank = new SPRankImpl(this, rank, level);
    }

    @Override
    public long getStatistic(PlayerStatistic statistic) {
        return this.statistic.getOrDefault(statistic, 0L);
    }

    @Override
    public void setStatistic(PlayerStatistic statistic, long value) {
        this.statistic.put(statistic, value);
    }
}
