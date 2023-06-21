package com.github.tezvn.starpvp.core.commands.arguments;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.core.gui.TopRanking;
import com.google.common.collect.Lists;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class TopArgument extends AbstractArgument{

    public TopArgument(SPPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "top";
    }

    @Override
    public String getDescription() {
        return "View top player";
    }

    @Override
    public String getUsage() {
        return "elo ranking/top";
    }

    @Override
    public List<String> getAliases() {
        return Lists.newArrayList("ranking");
    }

    @Override
    public void playerExecute(Player player, String[] args) {
        new TopRanking().open(player);
    }

    @Override
    public void consoleExecute(ConsoleCommandSender sender, String[] args) {

    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
