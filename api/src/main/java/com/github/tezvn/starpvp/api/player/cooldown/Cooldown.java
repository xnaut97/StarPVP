package com.github.tezvn.starpvp.api.player.cooldown;

import com.github.tezvn.starpvp.api.player.SPPlayer;

public interface Cooldown {

    CooldownType getType();

    long getDuration();

    long getStartTime();

    long getEndTime();

}
