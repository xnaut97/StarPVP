package com.github.tezvn.starpvp.api.player;

import org.bukkit.OfflinePlayer;

public interface SPPlayer {

    OfflinePlayer getPlayer();

    long getStarPoint();

    void setStarPoint(long starPoint);

    SPRank getRank();

    void setRank(RankTier rank, int level);

    long getStatistic(PlayerStatistic statistic);

    void setStatistic(PlayerStatistic statistic, long value);

}
