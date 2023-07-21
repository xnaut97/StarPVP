package com.github.tezvn.starpvp.core.commands;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.core.commands.elo.EloCommand;
import com.github.tezvn.starpvp.core.commands.rank.RankCommand;
import com.google.common.collect.Sets;

import java.util.Set;

public class CommandManager {

    private final SPPlugin plugin;

    private final Set<AbstractCommand<?>> commands = Sets.newHashSet();

    public CommandManager(SPPlugin plugin) {
        this.plugin = plugin;
        this.commands.add(new EloCommand(plugin));
        this.commands.add(new RankCommand(plugin));
        register();
    }

    public SPPlugin getPlugin() {
        return plugin;
    }

    public void register() {
        this.commands.forEach(AbstractCommand::register);
    }

    public void unregister() {
        this.commands.forEach(AbstractCommand::unregister);
    }
}
