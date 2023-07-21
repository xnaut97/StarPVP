package com.github.tezvn.starpvp.core.rank;

import com.github.tezvn.starpvp.api.rank.CompareResult;
import com.github.tezvn.starpvp.api.rank.RankManager;
import com.github.tezvn.starpvp.api.rank.SPRank;

import java.util.Comparator;

public class SPRankImpl implements SPRank {

    private final String id;

    private final long sp;

    private final String displayName;

    private final RankManager rankManager;

    public SPRankImpl(RankManager rankManager, String id, long sp, String displayName) {
        this.rankManager = rankManager;
        this.id = id;
        this.sp = sp;
        this.displayName = displayName;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public long getElo() {
        return this.sp;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public SPRank getNext() {
        return rankManager.getRanks().stream().filter(rank -> rank.getElo() > getElo())
                .min(Comparator.comparing(SPRank::getElo, Comparator.naturalOrder())).orElse(this);
    }

    @Override
    public SPRank getPrevious() {
        return rankManager.getRanks().stream().filter(rank -> rank.getElo() < getElo())
                .min(Comparator.comparing(SPRank::getElo, Comparator.naturalOrder())).orElse(this);

    }

    @Override
    public boolean isHighest() {
        return getNext().getElo() == getElo();
    }

    @Override
    public boolean isLowest() {
        return getPrevious().getElo() == getElo();
    }

    @Override
    public CompareResult compare(SPRank other) {
        if (getElo() > other.getElo())
            return CompareResult.HIGHER;
        if (getElo() < other.getElo())
            return CompareResult.LOWER;
        return CompareResult.EQUAL;
    }
}
