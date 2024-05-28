package com.github.tezvn.starpvp.core.commands.elo.arguments;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.core.gui.TopRanking;
import com.github.tezvn.starpvp.core.utils.ClickableText;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import com.github.tezvn.starpvp.core.utils.ObjectParser;
import com.google.common.collect.Lists;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public class TopArgument extends EloArgument {

    public TopArgument(SPPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "top";
    }

    @Override
    public String getDescription() {
        return "View top player";
    }

    @Override
    public String getUsage() {
        return "elo ranking/top [page]";
    }

    @Override
    public PermissionDefault getPermissionDefault() {
        return PermissionDefault.TRUE;
    }

    @Override
    public boolean allowConsole() {
        return false;
    }

    @Override
    public List<String> getAliases() {
        return Lists.newArrayList("ranking");
    }

    @Override
    public void playerExecute(Player player, String[] args) {
        int page = 0;
        if (args.length == 1) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (Exception e) {
                page = -1;
            }
            if (page == -1) {
                MessageUtils.sendMessage(player, "&cPlease type a valid number!");
                return;
            }
        }
        int amount = getPlugin().getConfig().getInt("leaderboard.list-amount", 10);
        int max = Math.min(amount * (page + 1), getPlayerManager().getPlayers().size());
        boolean canNext = amount * (page + 1) < getPlayerManager().getPlayers().size();
        List<SPPlayer> descendOrder = getPlayerManager().getPlayers().stream()
                .sorted((o1, o2) -> Long.compare(o2.getEloPoint(), o1.getEloPoint())).toList();

        player.spigot().sendMessage(createClickableButton("&7============[ &6&lBẢNG XẾP HẠNG &7]============", null));
        player.spigot().sendMessage(createClickableButton(" ", null));
        for(int i = amount * page; i < max; i++) {
            String color = "&f";
            switch (i) {
                case 0 -> color = "&c";
                case 1 -> color = "&e";
                case 2 -> color = "&a";
            }
            SPPlayer playerData = descendOrder.get(i);
            TextComponent position = createClickableButton(color + (i+1) + ". ", null, "&dThứ hạng");
            TextComponent name = createClickableButton(color + playerData.getPlayerName(), null, "&9Người chơi");
            TextComponent elo = createClickableButton("&b" + playerData.getEloPoint(), null, "&bĐiểm elo");
            TextComponent rank = createClickableButton("&b" + playerData.getRank().getDisplayName(), null, "&cCấp bậc");
            TextComponent spacing1 = createClickableButton(" ", null);
            TextComponent spacing2 = createClickableButton(" &7- ", null);
            player.spigot().sendMessage(spacing1, position, name, spacing2, elo, spacing2, rank);
        }
        TextComponent previousPage = createClickableButton("&e&l«",
                "/elo top " + (page - 1),
                "&7Trở về trang " + page);
        TextComponent nextPage = createClickableButton("&e&l»",
                "/elo top " + (page + 1),
                "&7Qua trang " + (page + 2));
        TextComponent pageInfo = createClickableButton(" &e&l" + (page + 1) + " ",
                null, "&7Bạn đang ở trang " + (page + 1));
        TextComponent spacing = new ClickableText("                       ").build();
        player.spigot().sendMessage(
                page > 0
                        ? canNext
                        ? new TextComponent[]{spacing, previousPage, pageInfo, nextPage}
                        : new TextComponent[]{spacing, previousPage, pageInfo}
                        : canNext
                        ? new TextComponent[]{spacing, pageInfo, nextPage}
                        : new TextComponent[]{spacing, pageInfo});
        MessageUtils.sendMessage(player, "&7=========================================");
    }

    @Override
    public void consoleExecute(ConsoleCommandSender sender, String[] args) {

    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return null;
    }

    private TextComponent createClickableButton(String name, String clickAction, String... hoverAction) {
        ClickableText clickableText = new ClickableText(name);
        if (hoverAction.length > 0)
            clickableText.setHoverAction(HoverEvent.Action.SHOW_TEXT, hoverAction);
        if (clickAction != null)
            clickableText.setClickAction(ClickEvent.Action.RUN_COMMAND, clickAction);
        return clickableText.build();
    }
}
