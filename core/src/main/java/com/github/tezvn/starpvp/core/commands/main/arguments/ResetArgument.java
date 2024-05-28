package com.github.tezvn.starpvp.core.commands.main.arguments;

import com.cryptomorin.xseries.XSound;
import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.PlayerStatistic;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import com.github.tezvn.starpvp.core.utils.ThreadWorker;
import com.github.tezvn.starpvp.core.utils.time.TimeUtils;
import com.google.common.collect.Lists;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class ResetArgument extends StarPVPArgument{

    public ResetArgument(SPPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "reset";
    }

    @Override
    public String getDescription() {
        return "Resets player statistic.";
    }

    @Override
    public String getUsage() {
        return "/starpvp reset [cooldown/combat/logout/penalty] [player/@all]";
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
            MessageUtils.sendMessage(sender, "&cVui lòng chọn kiểu dữ liệu muốn reset: &6cooldown/combat/logout/penalty");
            return;
        }
        if(args.length == 1) {
            MessageUtils.sendMessage(sender, "&cVui lòng nhập tên người chơi!");
            return;
        }
        String type = args[0];
        String name = args[1];
        if(name.equalsIgnoreCase("@all")) {
            getPlayerManager().getPlayers().forEach(p -> ThreadWorker.THREAD.submit(() -> {
                reset(sender, p, type);
                getPlayerManager().saveToDatabase(p.getUniqueId());
            }));
            return;
        }
        SPPlayer spPlayer = getPlayerManager().getPlayer(name);
        if (spPlayer == null) {
            MessageUtils.sendMessage(sender, "&cKhông thể tìm thấy người chơi &6" + name);
            return;
        }
        reset(sender, spPlayer, type);
        getPlayerManager().saveToDatabase(spPlayer.getUniqueId());
    }
    
    private void reset(CommandSender sender, SPPlayer spPlayer, String type) {
        switch (type.toLowerCase()) {
            case "cooldown" -> {
                spPlayer.removeCooldown();
                if (spPlayer.asPlayer() != null)
                    XSound.ENTITY_PLAYER_LEVELUP.play(spPlayer.asPlayer());
                MessageUtils.sendMessage(sender, "&6Đã xoá cooldown khỏi người chơi &a" + spPlayer.getPlayerName());
            }
            case "combat" -> {
                spPlayer.setStatistic(PlayerStatistic.COMBAT_TIMESTAMP, 0L);
                if(spPlayer.asPlayer() != null) {
                    MessageUtils.sendTitle(spPlayer.asPlayer(), "&a&l✔", "&7Bạn đã thoát giao tranh");
                    XSound.ENTITY_PLAYER_LEVELUP.play(spPlayer.asPlayer());
                }
                MessageUtils.sendMessage(sender, "&6Đã cho người chơi &a" + spPlayer.getPlayerName() + " &6thoát giao tranh!");
            }
            case "penalty" -> {
                spPlayer.setStatistic(PlayerStatistic.LAST_PENALTY_TIME, 0);
                spPlayer.setStatistic(PlayerStatistic.LAST_COMBAT_TIME, TimeUtils.newInstance().getNewTime());
                MessageUtils.sendMessage(sender, "&6Đã xoá bỏ án phạt của người chơi &a" + spPlayer.getPlayerName());
            }
            case "logout" -> {
                spPlayer.setStatistic(PlayerStatistic.COMBAT_LOGOUT_TIMES, 0L);
                MessageUtils.sendMessage(sender, "&6Đã đặt lại số lần thoát giao tranh của người chơi &a" + spPlayer.getPlayerName());
            }
            default -> MessageUtils.sendMessage(sender, "&cSai kiểu dữ liệu, vui lòng nhập lại!");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if(args.length == 1)
            return Lists.newArrayList("cooldown", "combat", "logout", "penalty")
                    .stream().filter(name -> name.startsWith(args[0])).toList();
        if(args.length == 2) {
            List<String> names = Lists.newArrayList("@all");
            names.addAll(getPlayerManager().getPlayers().stream()
                    .map(SPPlayer::getPlayerName).toList());
            return names.stream().filter(name -> name.startsWith(args[1])).toList();
        }
        return null;
    }
}
