package com.github.tezvn.starpvp.core.commands.main.arguments;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ReloadArgument extends StarPVPArgument {

    public ReloadArgument(SPPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Reload plugins";
    }

    @Override
    public String getUsage() {
        return "starpvp reload";
    }

    @Override
    public void playerExecute(Player player, String[] args) {
        execute(player);
    }

    @Override
    public void consoleExecute(ConsoleCommandSender sender, String[] args) {
        execute(sender);
    }

    private void execute(CommandSender sender) {
        getPlugin().reload();
        MessageUtils.sendMessage(sender, "&aReloaded plugin!");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
