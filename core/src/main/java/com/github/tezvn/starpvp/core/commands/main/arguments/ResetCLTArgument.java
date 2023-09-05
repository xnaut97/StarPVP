package com.github.tezvn.starpvp.core.commands.main.arguments;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.PlayerStatistic;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ResetCLTArgument extends StarPVPArgument {

    public ResetCLTArgument(SPPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "resetclt";
    }

    @Override
    public String getDescription() {
        return "Reset combat logged times";
    }

    @Override
    public String getUsage() {
        return "starpvp resetclt [player/@all]";
    }

    @Override
    public void playerExecute(Player player, String[] args) {
        execute(player, args);
    }

    @Override
    public void consoleExecute(ConsoleCommandSender sender, String[] args) {
        execute(sender, args);
    }

    private void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            MessageUtils.sendMessage(sender, "&cVui lòng nhập tên người chơi!");
            return;
        }
        String name = args[0];
        if(name.equalsIgnoreCase("@all")) {
            getPlayerManager().getPlayers().forEach(this::reset);
            MessageUtils.sendMessage(sender, "&aReset số lần thoát giao tranh của tất cả người chơi");
            return;
        }
        SPPlayer spPlayer = getPlayerManager().getPlayer(name);
        if (spPlayer == null) {
            MessageUtils.sendMessage(sender, "&cKhông thể tìm thấy người chơi &6" + name);
            return;
        }
        reset(spPlayer);
        MessageUtils.sendMessage(sender, "&aReset số lần thoát giao tranh của người chơi &6" + name);
    }

    private void reset(SPPlayer spPlayer) {
        spPlayer.setStatistic(PlayerStatistic.COMBAT_LOGOUT_TIMES, 0L);
        getPlayerManager().saveToDatabase(spPlayer.asOfflinePlayer());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
