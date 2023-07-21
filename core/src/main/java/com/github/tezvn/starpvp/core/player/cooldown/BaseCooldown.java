package com.github.tezvn.starpvp.core.player.cooldown;

import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.api.player.cooldown.Cooldown;
import com.github.tezvn.starpvp.api.player.cooldown.CooldownType;

public abstract class BaseCooldown implements Cooldown {

    private final long startTime;

    private final long endTime;

    private final CooldownType type;

    public BaseCooldown(long startTime, long endTime, CooldownType type) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.type = type;
    }

    @Override
    public CooldownType getType() {
        return type;
    }

    @Override
    public long getDuration() {
        return getEndTime() - getStartTime();
    }

    @Override
    public long getStartTime() {
        return this.startTime;
    }

    @Override
    public long getEndTime() {
        return this.endTime;
    }
}
