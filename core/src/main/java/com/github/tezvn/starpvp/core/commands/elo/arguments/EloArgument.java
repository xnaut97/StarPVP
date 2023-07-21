package com.github.tezvn.starpvp.core.commands.elo.arguments;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.core.commands.AbstractArgument;

public abstract class EloArgument extends AbstractArgument {
    public EloArgument(SPPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getPermission() {
        return "starpvp.command.elo." + getName();
    }

}
