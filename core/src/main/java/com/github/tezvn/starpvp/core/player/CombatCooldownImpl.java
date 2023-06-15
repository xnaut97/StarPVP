package com.github.tezvn.starpvp.core.player;

import com.github.tezvn.starpvp.api.player.CombatCooldown;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.core.utils.time.TimeUtils;

public class CombatCooldownImpl implements CombatCooldown {

    private final SPPlayer player;

    private final long since;

    private final long end;

    public CombatCooldownImpl(SPPlayer player, long end) {
        this(player, TimeUtils.newInstance().getNewTime(), end);
    }

    public CombatCooldownImpl(SPPlayer player, long since, long end) {
        this.player = player;
        this.since = since;
        this.end = end;
    }

    @Override
    public SPPlayer getPlayer() {
        return player;
    }

    @Override
    public long getDuration() {
        return end - since;
    }

    @Override
    public long getStartTime() {
        return this.since;
    }

    @Override
    public long getEndTime() {
        return this.end;
    }

}
