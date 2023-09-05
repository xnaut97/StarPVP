package com.github.tezvn.starpvp.core.commands.elo;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.core.commands.AbstractCommand;
import com.github.tezvn.starpvp.core.commands.elo.arguments.EloArgument;
import com.github.tezvn.starpvp.core.utils.ClassFinder;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import com.google.common.collect.Lists;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.stream.Collectors;

public class EloCommand extends AbstractCommand<SPPlugin> {
    public EloCommand(SPPlugin plugin) {
        super(plugin, "elo", "View player's elo point", "/elo", Lists.newArrayList("ep", "epoint"));
        registerArguments(new ClassFinder(plugin).setPackage(EloArgument.class).removeClass(EloArgument.class).find().stream().map(c -> {
            try {
                return c.getConstructor(SPPlugin.class).newInstance(plugin);
            } catch (Exception e) {
                plugin.getLogger().severe("Error while registering argument '" + c.getSimpleName() + "' for command '" + getName() + "'");
                return null;
            }
        }).filter(Objects::nonNull).distinct().toArray(CommandArgument[]::new));
    }

    @Override
    public void onSingleExecute(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender)
            return;
        Player player = (Player) sender;
        SPPlayer spPlayer = getPlugin().getPlayerManager().getPlayer(player);
        if (spPlayer == null) {
            MessageUtils.sendMessage(player, "&7[&4&l!&7] &cĐã xảy ra lỗi trong quá trình kiểm tra điểm của bạn.");
            return;
        }
        MessageUtils.sendMessage(player,
                "&f- Điểm: &6" + spPlayer.getEloPoint(),
                "&f- Cấp bậc: &b" + spPlayer.getRank().getDisplayName());
    }


}
