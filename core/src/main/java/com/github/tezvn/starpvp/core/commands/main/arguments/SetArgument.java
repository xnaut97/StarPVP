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

public class SetArgument extends StarPVPArgument {
    public SetArgument(SPPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "set";
    }

    @Override
    public String getDescription() {
        return "Set data for specific player.";
    }

    @Override
    public String getUsage() {
        return "/starpvp set [combat/penalty] [time-millis] [player/@all]";
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
            MessageUtils.sendMessage(sender, "&cVui lòng chọn kiểu dữ liệu muốn set: &6combat/penalty");
            return;
        }
        String type = args[0];

        if(args.length == 1) {
            MessageUtils.sendMessage(sender, "&cVui lòng nhập thời gian (định dạng time millis)");
            return;
        }
        long time = Long.parseLong(args[1]);
        if(args.length == 2) {
            MessageUtils.sendMessage(sender, "&cVui lòng nhập tên người chơi!");
            return;
        }
        String name = args[2];
        if(name.equalsIgnoreCase("@all")) {
            getPlayerManager().getPlayers().forEach(p -> ThreadWorker.THREAD.submit(() -> {
                setData(p, type, time);
                getPlayerManager().saveToDatabase(p.getUniqueId());
            }));
            MessageUtils.sendMessage(sender, "&cĐã thiết lập dữ liệu cho tất cả nguười chơi!");
            return;
        }
        SPPlayer spPlayer = getPlayerManager().getPlayer(name);
        if (spPlayer == null) {
            MessageUtils.sendMessage(sender, "&cKhông thể tìm thấy người chơi &6" + name);
            return;
        }
        boolean success = setData(spPlayer, type, time);
        if(success)
            MessageUtils.sendMessage(sender, "&aĐã thíết lập dữ liệu cho người chơi " + name);
        getPlayerManager().saveToDatabase(spPlayer.getUniqueId());
    }

    private boolean setData(SPPlayer spPlayer, String type, long time) {
        switch (type.toLowerCase()) {
            case "combat" -> {
                spPlayer.setStatistic(PlayerStatistic.LAST_COMBAT_TIME, time);
            }
            case "penalty" -> {
                spPlayer.setStatistic(PlayerStatistic.LAST_PENALTY_TIME, time);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if(args.length == 1)
            return Lists.newArrayList("combat", "penalty").stream().filter(name -> name.startsWith(args[0])).toList();
        if(args.length == 2)
            return Collections.singletonList("time-millis");
        if(args.length == 3) {
            List<String> names = Lists.newArrayList("@all");
            names.addAll(getPlayerManager().getPlayers().stream()
                    .map(SPPlayer::getPlayerName).toList());
            return names.stream().filter(name -> name.startsWith(args[2])).toList();
        }
        return null;
    }
}
