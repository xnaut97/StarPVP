package com.github.tezvn.starpvp.api.rank;

public interface SPRank {

    String getId();

    long getSP();

    String getDisplayName();

    SPRank getNext();

    SPRank getPrevious();

    boolean isHighest();

    boolean isLowest();

}
