package com.github.tezvn.starpvp.core.rank;

import com.github.tezvn.starpvp.api.rank.SPRank;

public class PenaltyData {

    private final int activeDays;

    private final int period;

    private final long eloLost;

    protected PenaltyData(int activeDays, int period, long eloLost) {
        this.activeDays = activeDays;
        this.period = period;
        this.eloLost = eloLost;
    }

    public int getActiveDays() {
        return activeDays;
    }

    public int getPeriod() {
        return period;
    }

    public long getEloLost() {
        return eloLost;
    }

}
