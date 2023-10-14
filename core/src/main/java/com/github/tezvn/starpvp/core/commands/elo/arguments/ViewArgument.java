package com.github.tezvn.starpvp.core.commands.elo.arguments;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;

public class ViewArgument extends EloArgument {

    public ViewArgument(SPPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "view";
    }

    @Override
    public String getDescription() {
        return "View other player's SP";
    }

    @Override
    public String getUsage() {
        return "elo view [player]";
    }

    @Override
    public void playerExecute(Player player, String[] args) {
        if(args.length == 0) {
            MessageUtils.sendMessage(player, "&cVui lòng nhập tên người chơi!");
            return;
        }
        String name = args[0];
        SPPlayer spPlayer = getPlayerManager().getPlayer(name);
        if(spPlayer == null) {
            MessageUtils.sendMessage(player, "&cKhông thể tìm thấy người chơi tên &6" + name);
            return;
        }
        MessageUtils.sendMessage(player,
                "&f- Điểm: &6" + spPlayer.getEloPoint(),
                "&f- Cấp bậc: &b" + spPlayer.getRank().getDisplayName());
    }

    @Override
    public void consoleExecute(ConsoleCommandSender sender, String[] args) {

    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
