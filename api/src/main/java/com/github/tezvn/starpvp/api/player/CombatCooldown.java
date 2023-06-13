package com.github.tezvn.starpvp.api.player;

public interface CombatCooldown {

    SPPlayer getPlayer();

    long getDuration();

    long getStartTime();

    long getEndTime();

}
