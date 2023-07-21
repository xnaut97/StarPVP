package com.github.tezvn.starpvp.core.commands.rank.arguments;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ResetArgument extends RankArgument {
    public ResetArgument(SPPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "reset";
    }

    @Override
    public String getDescription() {
        return "Resets player's rank.";
    }

    @Override
    public String getUsage() {
        return "rank reset [player]";
    }

    @Override
    public void playerExecute(Player player, String[] args) {

    }

    @Override
    public void consoleExecute(ConsoleCommandSender sender, String[] args) {

    }

    private void execute(CommandSender sender, String[] args) {
        if(args.length == 0) {
            MessageUtils.sendMessage(sender, "&cVui lòng nhập tên người chơi");
            return;
        }
        String playerName = args[0];
        OfflinePlayer target = Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(p -> p.getName() != null && p.getName().equals(playerName))
                .findAny().orElse(null);
        if(target == null) {
            MessageUtils.sendMessage(sender, "&cKhông thể tìm thấy người chơi &6" + playerName);
            return;
        }
        SPPlayer spPlayer = getPlayerManager().getPlayer(target);
        if(spPlayer == null) {
            MessageUtils.sendMessage(sender, "&cKhông thể tìm thấy dữ liệu của người chơi &6" + playerName);
            return;
        }
        spPlayer.setRank(getRankManager().getLowestRank());
        MessageUtils.sendMessage(sender, "&6Đã đặt lại cấp bậc của người chơi &b" + spPlayer.getPlayerName());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if(args.length == 1)
            return Arrays.stream(Bukkit.getOfflinePlayers())
                    .filter(p -> p.getName() != null && p.getName().startsWith(args[0]))
                    .map(OfflinePlayer::getName).collect(Collectors.toList());
        return null;
    }
}
