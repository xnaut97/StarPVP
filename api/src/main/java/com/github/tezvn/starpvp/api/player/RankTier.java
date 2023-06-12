package com.github.tezvn.starpvp.api.player;

public enum RankTier {
    GRASS,
    COAL,
    IRON,
    GOLD,
    EMERALD,
    NETHERITE,
    STARISE;

    public RankTier getNext() {
        return RankTier.values()[Math.min(this.ordinal()+1, RankTier.values().length-1)];
    }

    public RankTier getPrevious() {
        return RankTier.values()[Math.max(0, this.ordinal()-1)];
    }
}
