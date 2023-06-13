package com.github.tezvn.starpvp.api.player;

import com.github.tezvn.starpvp.api.rank.SPRank;
import org.bukkit.OfflinePlayer;

public interface SPPlayer {

    OfflinePlayer getPlayer();

    long getStarPoint();

    void setStarPoint(long starPoint);

    SPRank getRank();

    void setRank(SPRank rank);

    void setRank(SPRank rank, boolean resetSP);

    long getStatistic(PlayerStatistic statistic);

    void setStatistic(PlayerStatistic statistic, long value);
}
