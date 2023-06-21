package com.github.tezvn.starpvp.core.commands;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

public class CommandManager {

    private final SPPlugin plugin;

    private final Set<AbstractCommand> commmands = Sets.newHashSet();

    public CommandManager(SPPlugin plugin) {
        this.plugin = plugin;
        this.commmands.add(new SPCommand(plugin));
        register();
    }

    public void register() {
        this.commmands.forEach(AbstractCommand::register);
    }

    public void unregister() {
        this.commmands.forEach(AbstractCommand::unregister);
    }
}
