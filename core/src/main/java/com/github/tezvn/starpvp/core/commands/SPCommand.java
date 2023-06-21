package com.github.tezvn.starpvp.core.commands;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.core.commands.arguments.TopArgument;
import com.github.tezvn.starpvp.core.commands.arguments.ViewArgument;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import com.google.common.collect.Lists;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class SPCommand extends AbstractCommand<SPPlugin> {
    public SPCommand(SPPlugin plugin) {
        super(plugin, "elo", "View player's elo point", "/elo", Lists.newArrayList("ep", "epoint"));
        addSubCommand(new ViewArgument(plugin));
        addSubCommand(new TopArgument(plugin));
    }

    @Override
    public void onSingleExecute(CommandSender sender) {
        if(sender instanceof ConsoleCommandSender)
            return;
        Player player = (Player) sender;
        SPPlayer spPlayer = getPlugin().getPlayerManager().getPlayer(player);
        if(spPlayer == null) {
            MessageUtils.sendMessage(player, "&7[&4&l!&7] &cĐã xảy ra lỗi trong quá trình kiểm tra điểm của bạn.");
            return;
        }
        MessageUtils.sendMessage(player,
                "&f- Điểm: &6" + spPlayer.getEloPoint(),
                "&f- Cấp bậc: &b" + spPlayer.getRank().getDisplayName());
    }

}
