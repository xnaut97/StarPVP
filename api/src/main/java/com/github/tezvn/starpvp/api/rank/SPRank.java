package com.github.tezvn.starpvp.api.rank;

public interface SPRank {

    String getId();

    long getElo();

    String getDisplayName();

    SPRank getNext();

    SPRank getPrevious();

    boolean isHighest();

    boolean isLowest();

    CompareResult compare(SPRank other);

}
