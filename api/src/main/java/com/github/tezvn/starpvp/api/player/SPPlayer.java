package com.github.tezvn.starpvp.api.player;

import com.github.tezvn.starpvp.api.rank.SPRank;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.UUID;

public interface SPPlayer {

    UUID getUniqueId();

    String getPlayerName();

    OfflinePlayer getPlayer();

    long getStarPoint();

    void setStarPoint(long starPoint);

    SPRank getRank();

    void setRank(SPRank rank);

    void setRank(SPRank rank, boolean resetSP);

    long getStatistic(PlayerStatistic statistic);

    void setStatistic(PlayerStatistic statistic, long value);

    long getPenaltyTimes();

    boolean isPenalty();

    Map<String, Object> serialize();

}
