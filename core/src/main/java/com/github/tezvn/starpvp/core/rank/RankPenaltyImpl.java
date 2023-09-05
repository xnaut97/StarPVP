package com.github.tezvn.starpvp.core.rank;

import com.github.tezvn.starpvp.api.rank.RankPenalty;
import com.github.tezvn.starpvp.api.rank.SPRank;

public final class RankPenaltyImpl implements RankPenalty {

    private final SPRank rank;

    private final int activeDays;

    private final int period;

    private final long eloLost;

    public RankPenaltyImpl(SPRank rank, PenaltyData data) {
        this.rank = rank;
        this.activeDays = data.getActiveDays();
        this.period = data.getPeriod();
        this.eloLost = data.getEloLost();
    }

    @Override
    public SPRank getRank() {
        return rank;
    }

    @Override
    public int getActiveDays() {
        return activeDays;
    }

    @Override
    public int getPeriod() {
        return period;
    }

    @Override
    public long getEloLost() {
        return eloLost;
    }
}
