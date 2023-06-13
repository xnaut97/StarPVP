package com.github.tezvn.starpvp.core.player;

import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.core.utils.MathUtils;
import com.google.common.base.Preconditions;

public class EloProcessor {

    public SPPlayer loser;

    private SPPlayer winner;

    private long oldLoserSP;

    private long oldWinnerSP;

    public EloProcessor() {
    }

    public SPPlayer getWinner() {
        return winner;
    }

    public long getOldWinnerSP() {
        return oldWinnerSP;
    }

    public long getNewWinnerSP() {
        validate();

        double Qa = MathUtils.roundDouble(Math.pow(10, (double) loser.getStarPoint() /400), 3);
        double Qb = MathUtils.roundDouble(Math.pow(10, (double) winner.getStarPoint() /400), 3);
        double Eb = MathUtils.roundDouble(Qb/(Qa+Qb), 3);
        return MathUtils.roundUp(winner.getStarPoint() + getKFactor(winner)*(1 - Eb));
    }

    public EloProcessor setWinner(SPPlayer winner) {
        this.winner = winner;
        this.oldWinnerSP = winner.getStarPoint();
        return this;
    }

    public SPPlayer getLoser() {
        return loser;
    }

    public long getOldLoserSP() {
        return oldLoserSP;
    }

    public EloProcessor setLoser(SPPlayer loser) {
        this.loser = loser;
        this.oldLoserSP = loser.getStarPoint();
        return this;
    }

    public long getNewLoserSP() {
        validate();

        double Qa = MathUtils.roundDouble(Math.pow(10, (double) loser.getStarPoint() /400), 3);
        double Qb = MathUtils.roundDouble(Math.pow(10, (double) winner.getStarPoint() /400), 3);
        double Ea = MathUtils.roundDouble(Qa/(Qa+Qb), 3);
        return MathUtils.roundUp(loser.getStarPoint() + getKFactor(loser)*(0 - Ea));
    }

    private int getKFactor(SPPlayer player) {
        long sp = player.getStarPoint();
        if(sp > 2400)
            return 10;
        if(sp >= 2000 && sp < 2400)
            return 15;
        if(sp >= 1600 && sp < 2000)
            return 20;
        return 25;
    }

    private void validate() {
        Preconditions.checkNotNull(getWinner(), "winner cannot be null");
        Preconditions.checkNotNull(getLoser(), "loser cannot be null");
    }
}
