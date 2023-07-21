package com.github.tezvn.starpvp.core.commands.rank.arguments;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.core.commands.AbstractArgument;

public abstract class RankArgument extends AbstractArgument {

    public RankArgument(SPPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getPermission() {
        return "starpvp.command.rank." + getName();
    }

}
