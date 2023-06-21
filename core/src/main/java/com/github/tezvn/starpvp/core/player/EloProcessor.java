package com.github.tezvn.starpvp.core.player;

import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.core.utils.MathUtils;
import com.google.common.base.Preconditions;

public class EloProcessor {

    public SPPlayer loser;

    private SPPlayer winner;

    private long oldLoserEP;

    private long oldWinnerEP;

    public EloProcessor() {
    }

    public SPPlayer getWinner() {
        return winner;
    }

    public long getOldWinnerEP() {
        return oldWinnerEP;
    }

    public long getNewWinnerEP() {
        validate();
        double Qa = MathUtils.roundDouble(Math.pow(10, (double) loser.getEloPoint() /400), 3);
        double Qb = MathUtils.roundDouble(Math.pow(10, (double) winner.getEloPoint() /400), 3);
        double Eb = MathUtils.roundDouble(Qb/(Qa+Qb), 3);
        return MathUtils.roundUp(winner.getEloPoint() + getKFactor(winner)*(1 - Eb));
    }

    public EloProcessor setWinner(SPPlayer winner) {
        this.winner = winner;
        this.oldWinnerEP = winner.getEloPoint();
        return this;
    }

    public SPPlayer getLoser() {
        return loser;
    }

    public long getOldLoserEP() {
        return oldLoserEP;
    }

    public EloProcessor setLoser(SPPlayer loser) {
        this.loser = loser;
        this.oldLoserEP = loser.getEloPoint();
        return this;
    }

    public long getNewLoserEP() {
        validate();
        double Qa = MathUtils.roundDouble(Math.pow(10, (double) loser.getEloPoint() /400), 3);
        double Qb = MathUtils.roundDouble(Math.pow(10, (double) winner.getEloPoint() /400), 3);
        double Ea = MathUtils.roundDouble(Qa/(Qa+Qb), 3);
        return MathUtils.roundUp(loser.getEloPoint() + getKFactor(loser)*(0 - Ea));
    }

    private int getKFactor(SPPlayer player) {
        long sp = player.getEloPoint();
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
