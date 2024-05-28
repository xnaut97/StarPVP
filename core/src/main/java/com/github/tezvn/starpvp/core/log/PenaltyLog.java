package com.github.tezvn.starpvp.core.log;

import org.bukkit.plugin.Plugin;

public class PenaltyLog extends BaseLog{
    public PenaltyLog(Plugin plugin) {
        super(plugin, LogType.PENALTY);
    }
}
