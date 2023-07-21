package com.github.tezvn.starpvp.core.commands.rank.arguments;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;

public class ListArgument extends RankArgument {
    public ListArgument(SPPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "Views all rank";
    }

    @Override
    public String getUsage() {
        return "rank list";
    }

    @Override
    public PermissionDefault getPermissionDefault() {
        return PermissionDefault.TRUE;
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
        MessageUtils.sendMessage(sender, "&6Danh sách cấp bậc:");
        getRankManager().getRanks().stream()
                .sorted((r1, r2) -> (int) (r1.getElo() - r2.getElo()))
                .map(rank -> "&f• " + rank.getDisplayName() + (sender instanceof Player ? "&7« &aBạn đang ở bậc này" : ""))
                .forEachOrdered(r -> MessageUtils.sendMessage(sender, r));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
