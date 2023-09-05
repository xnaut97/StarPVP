package com.github.tezvn.starpvp.api.rank;

import javax.annotation.Nullable;

public interface SPRank {

    /**
     * Get rank id.
     */
    String getId();

    /**
     * Get rank elo.
     */
    long getElo();

    /**
     * Get rank display name.
     */
    String getDisplayName();

    /**
     * Check if this rank is highest.
     */
    boolean isHighest();

    /**
     * Check if this rank is lowest.
     */
    boolean isLowest();

    /**
     * Get rank penalty, may be null if rank have no penalty.
     * @return {@link RankPenalty} or null
     */
    @Nullable
    RankPenalty getPenalty();

}
