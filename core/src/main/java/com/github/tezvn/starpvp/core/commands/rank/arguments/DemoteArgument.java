package com.github.tezvn.starpvp.core.commands.rank.arguments;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.api.rank.SPRank;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DemoteArgument extends RankArgument {

    public DemoteArgument(SPPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "demote";
    }

    @Override
    public String getDescription() {
        return "Demotes player rank";
    }

    @Override
    public String getUsage() {
        return "rank demote [player] [reset-elo]";
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
        if(args.length == 1) {
            MessageUtils.sendMessage(sender, "&cVui lòng nhập tên cấp bậc");
            return;
        }
        SPRank oldRank = spPlayer.getRank();
        SPRank rank = oldRank.getPrevious();
        if(rank.isLowest()) {
            MessageUtils.sendMessage(sender, "&cNgười chơi này đã ở cấp bậc thấp nhất rồi!");
            return;
        }
        boolean resetElo = args.length == 3 && Boolean.parseBoolean(args[2]);
        spPlayer.setRank(rank, resetElo);
        MessageUtils.sendMessage(sender, "&6" + playerName + ": " + "&c▼ " + rank.getDisplayName());
        getPlayerManager().saveToDatabase(target);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if(args.length == 1)
            return Arrays.stream(Bukkit.getOfflinePlayers())
                    .filter(p -> p.getName() != null && p.getName().startsWith(args[0]))
                    .map(OfflinePlayer::getName).collect(Collectors.toList());
        if(args.length == 2)
            return Collections.singletonList("true/false");
        return null;
    }
}
