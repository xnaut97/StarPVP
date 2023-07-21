package com.github.tezvn.starpvp.core.player.cooldown;

import com.github.tezvn.starpvp.api.player.cooldown.CooldownType;
import com.github.tezvn.starpvp.api.player.cooldown.LogoutCooldown;
import com.github.tezvn.starpvp.core.utils.time.TimeUtils;

public class LogoutCooldownImpl extends BaseCooldown implements LogoutCooldown {

    public LogoutCooldownImpl(long startTime, long endTime) {
        super(startTime, endTime, CooldownType.COMBAT_LOGOUT);
    }

    public LogoutCooldownImpl(long endTime) {
        super(TimeUtils.newInstance().getNewTime(), endTime, CooldownType.COMBAT_LOGOUT);
    }

}
