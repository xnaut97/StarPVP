package com.github.tezvn.starpvp.api.rank;

import javax.annotation.Nullable;
import java.util.List;

public interface RankManager {

    List<SPRank> getRanks();

    @Nullable
    SPRank getRank(String id);

    SPRank getLowestRank();

    SPRank getHighestRank();

    void reload();

}
