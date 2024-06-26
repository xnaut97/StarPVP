package com.github.tezvn.starpvp.core.commands.elo.arguments;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.api.rank.SPRank;
import com.github.tezvn.starpvp.core.commands.elo.arguments.EloArgument;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import com.github.tezvn.starpvp.core.utils.ObjectParser;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SetArgument extends EloArgument {

    public SetArgument(SPPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "set";
    }

    @Override
    public String getDescription() {
        return "Sets elo for player";
    }

    @Override
    public String getUsage() {
        return "elo set [player] [amount]";
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
        SPPlayer spPlayer = getPlayerManager().getPlayer(name);
        if (spPlayer == null) {
            MessageUtils.sendMessage(sender, "&cKhông thể tìm thấy người chơi &6" + name);
            return;
        }
        if (args.length == 1) {
            MessageUtils.sendMessage(sender, "&cVui lòng nhập số điểm elo");
            return;
        }
        long elo = ObjectParser.parseNumber(args[1]).longValue();
        long oldElo = spPlayer.getEloPoint();
        SPRank oldRank = spPlayer.getRank();
        spPlayer.setEloPoint(elo);
        MessageUtils.sendMessage(sender, "&6" + spPlayer.getPlayerName() + ": &c" + oldElo + " &7» &a" + spPlayer.getEloPoint());
        boolean promoted = oldRank.getElo() < spPlayer.getRank().getElo();
        MessageUtils.sendMessage(sender, "&6Cấp bậc của " + spPlayer.getPlayerName() + ": &b" + spPlayer.getRank().getDisplayName()
                + (promoted ? "&a▲" : "&c▼"));
        getPlayerManager().saveToDatabase(spPlayer.asOfflinePlayer());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1)
            return getPlayerManager().getPlayers().stream()
                    .map(SPPlayer::getPlayerName)
                    .filter(name -> name != null && name.startsWith(args[0]))
                    .collect(Collectors.toList());
        if (args.length == 2)
            return Collections.singletonList("amount");
        return null;
    }
}
