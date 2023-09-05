package com.github.tezvn.starpvp.core.commands.main.arguments;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.core.commands.AbstractArgument;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class StarPVPArgument extends AbstractArgument {
    public StarPVPArgument(SPPlugin plugin) {
        super(plugin);
    }

}
