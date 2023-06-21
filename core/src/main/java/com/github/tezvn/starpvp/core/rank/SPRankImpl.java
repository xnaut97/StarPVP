package com.github.tezvn.starpvp.core.rank;

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
    public long getSP() {
        return this.sp;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public SPRank getNext() {
        return rankManager.getRanks().stream().filter(rank -> rank.getSP() > getSP())
                .max(Comparator.comparing(SPRank::getSP, Comparator.naturalOrder())).orElse(this);
    }

    @Override
    public SPRank getPrevious() {
        return rankManager.getRanks().stream().filter(rank -> rank.getSP() > getSP())
                .min(Comparator.comparing(SPRank::getSP, Comparator.naturalOrder())).orElse(this);

    }

    @Override
    public boolean isHighest() {
        return getNext().getSP() == getSP();
    }

    @Override
    public boolean isLowest() {
        return getPrevious().getSP() == getSP();
    }

}
