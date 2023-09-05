package com.github.tezvn.starpvp.api.rank;

public interface RankPenalty {

    /**
     * Get rank from penalty
     * @return {@link SPRank}
     */
    SPRank getRank();

    /**
     * Get days to active penalty
     */
    int getActiveDays();

    /**
     * Get elo lost
     * @return
     */
    long getEloLost();

    /**
     * Get period day to subtract elo
     */
    int getPeriod();

}
