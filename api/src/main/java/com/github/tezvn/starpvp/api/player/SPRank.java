package com.github.tezvn.starpvp.api.player;

public interface SPRank {

    RankTier getTier();

    int getLevel();

    boolean promote();

    boolean demote();

    boolean demote(boolean resetSP);

}
