package com.github.tezvn.starpvp.core.rank;

import com.github.tezvn.starpvp.api.rank.RankManager;
import com.github.tezvn.starpvp.api.rank.RankPenalty;
import com.github.tezvn.starpvp.api.rank.SPRank;
import org.jetbrains.annotations.Nullable;

public class SPRankImpl implements SPRank {

    private final String id;

    private final long elo;

    private final String displayName;

    private final RankManager rankManager;

    private final RankPenalty penalty;

    public SPRankImpl(RankManager rankManager, String id, long elo, String displayName) {
        this(rankManager, id, elo, displayName, null);
    }

    public SPRankImpl(RankManager rankManager, String id, long elo, String displayName, PenaltyData data) {
        this.rankManager = rankManager;
        this.id = id;
        this.elo = elo;
        this.displayName = displayName;
        this.penalty = data == null ? null :  new RankPenaltyImpl(this, data);
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public long getElo() {
        return this.elo;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public boolean isHighest() {
        return rankManager.getHighestRank().getElo() == getElo();
    }

    @Override
    public boolean isLowest() {
        return rankManager.getLowestRank().getElo() == getElo();
    }

    @Nullable
    @Override
    public RankPenalty getPenalty() {
        return penalty;
    }

}
