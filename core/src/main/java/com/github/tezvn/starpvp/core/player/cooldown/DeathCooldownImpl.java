package com.github.tezvn.starpvp.core.player.cooldown;

import com.github.tezvn.starpvp.api.player.cooldown.CooldownType;
import com.github.tezvn.starpvp.api.player.cooldown.DeathCooldown;
import com.github.tezvn.starpvp.core.utils.time.TimeUtils;

public class DeathCooldownImpl extends BaseCooldown implements DeathCooldown {

    public DeathCooldownImpl(long start, long end) {
        super(start, end, CooldownType.COMBAT_DEATH);
    }

    public DeathCooldownImpl(long end) {
        super(TimeUtils.newInstance().getNewTime(), end, CooldownType.COMBAT_DEATH);
    }

}
