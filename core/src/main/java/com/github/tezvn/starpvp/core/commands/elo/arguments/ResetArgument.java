package com.github.tezvn.starpvp.core.commands.elo.arguments;

import com.cryptomorin.xseries.XSound;
import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class ResetArgument extends EloArgument {
    public ResetArgument(SPPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "reset";
    }

    @Override
    public String getDescription() {
        return "Reset player's elo point";
    }

    @Override
    public String getUsage() {
        return "elo reset [player]";
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
            MessageUtils.sendMessage(sender, "&cVui lòng nhập tên người chơi!");
            return;
        }
        String name = args[0];
        SPPlayer spPlayer = getPlayerManager().getPlayer(name);
        if(spPlayer == null) {
            MessageUtils.sendMessage(sender, "&cKhông thể tìm thấy người chơi &6" + name);
            return;
        }
        spPlayer.setEloPoint(1000);
        MessageUtils.sendMessage(sender, "&6Đã đặt lại số điểm về 0 cho người chơi &b" + spPlayer.getPlayerName());
        if(spPlayer.asPlayer() != null) {
            MessageUtils.sendMessage(spPlayer.asPlayer(), "&6Điểm elo của bạn đã bị reset!");
            XSound.BLOCK_ENCHANTMENT_TABLE_USE.play(spPlayer.asPlayer());
        }
        getPlayerManager().saveToDatabase(spPlayer.asOfflinePlayer());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if(args.length == 1)
            return getPlayerManager().getPlayers().stream()
                    .map(SPPlayer::getPlayerName)
                    .filter(name -> name != null && name.startsWith(args[0]))
                    .collect(Collectors.toList());
        return null;
    }
}
