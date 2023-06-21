package com.github.tezvn.starpvp.core.commands.arguments;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.PlayerManager;
import com.github.tezvn.starpvp.core.commands.AbstractCommand;
import com.google.common.collect.Lists;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;

public abstract class AbstractArgument extends AbstractCommand.CommandArgument {

    private final SPPlugin plugin;

    public AbstractArgument(SPPlugin plugin) {
        this.plugin = plugin;
    }

    public SPPlugin getPlugin() {
        return plugin;
    }

    public PlayerManager getPlayerManager() {
        return getPlugin().getPlayerManager();
    }

    @Override
    public String getPermission() {
        return "starpvp.command." + getName();
    }

    @Override
    public String getPermissionDescription() {
        return "";
    }

    @Override
    public PermissionDefault getPermissionDefault() {
        return PermissionDefault.TRUE;
    }

    @Override
    public List<String> getAliases() {
        return Lists.newArrayList();
    }

    @Override
    public boolean allowConsole() {
        return false;
    }

}
