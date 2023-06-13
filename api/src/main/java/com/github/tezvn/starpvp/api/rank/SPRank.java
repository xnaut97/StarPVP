package com.github.tezvn.starpvp.api.rank;

public enum SPRank {
    COAL(1000),
    COPPER(1200),
    IRON(1600),
    GOLD(1800),
    DIAMOND(2000),
    EMERALD(2200),
    NETHERITE(2400),
    STARISE(2500);

    private final int sp;

    SPRank(int sp) {
        this.sp = sp;
    }

    public int getSP() {
        return sp;
    }

    public SPRank getNext() {
        return SPRank.values()[Math.min(this.ordinal()+1, SPRank.values().length-1)];
    }

    public SPRank getPrevious() {
        return SPRank.values()[Math.max(0, this.ordinal()-1)];
    }

    public boolean isHighest() {
        return this.ordinal() == SPRank.values().length-1;
    }

    public boolean isLowest() {
        return this.ordinal() == 0;
    }
}
