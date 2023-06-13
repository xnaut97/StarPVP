package com.github.tezvn.starpvp.core.utils;

import com.github.tezvn.starpvp.core.SPPluginImpl;
import com.github.tezvn.starpvp.core.utils.time.TimeUnits;
import com.github.tezvn.starpvp.core.utils.time.TimeUtils;
import com.google.common.collect.Maps;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MessageUtils {

    private static final Map<UUID, Map<String, Long>> cooldowns = Maps.newHashMap();

    public static void sendMessage(Player player, String... msg) {
        sendMessage(player, 0, 0, msg);
    }

    public static void sendCooldownMessage(Player player, int cooldown, String... msg) {
        sendMessage(player, 0, cooldown, msg);
    }

    public static void sendDelayMessage(Player player, int delay, String... msg) {
        sendMessage(player, 0, delay, msg);
    }

    public static void sendMessage(CommandSender sender, String... msg) {
        for (String s : msg) {
            sender.sendMessage(s.replace("&", "§"));
        }
    }

    public static void broadcast(String... messages) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendMessage(player, messages);
        }
    }

    public static void sendMessage(Player player, int delay, int cooldown, String... msg) {
        Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(SPPluginImpl.class), () -> {
            Map<String, Long> map = cooldowns.getOrDefault(player.getUniqueId(), Maps.newHashMap());
            for (String s : msg) {
                long date = map.getOrDefault(s, -1L);
                if (date > System.currentTimeMillis())
                    continue;
                player.sendMessage(s.replace("&", "§"));
                if (cooldown > 0)
                    map.put(s, TimeUtils.of(System.currentTimeMillis()).add(TimeUnits.SECOND, cooldown).getNewTime());
            }
            cooldowns.put(player.getUniqueId(), map);
        }, 20L * delay);
    }

    public static void sendRepeatedMessage(Player player, int delay, int period, boolean async,
                                           Function<Player, Boolean> condition, String... msg) {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || (condition != null && condition.apply(player))) {
                    cancel();
                    return;
                }
                if (msg != null || msg.length > 0)
                    for (String s : msg) {
                        player.sendMessage(color(s));
                    }
            }
        };
        if (async)
            runnable.runTaskTimerAsynchronously(JavaPlugin.getPlugin(SPPluginImpl.class),
                    20L * delay, 20L * period);
        else
            runnable.runTaskTimer(JavaPlugin.getPlugin(SPPluginImpl.class),
                    20L * delay, 20L * period);
    }

    public static void sendTitle(Player player, String title) {
        sendTitle(player, title, "");
    }

    public static void sendTitle(Player player, String title, String description) {
        player.sendTitle(color(title), color(description));
    }

    public static String color(String str) {
        return str.replace("&", "§");
    }

    public static List<String> color(String... msg) {
        return Arrays.stream(msg).map(MessageUtils::color).collect(Collectors.toList());
    }

    public static List<String> color(List<String> list) {
        return color(list.toArray(new String[0]));
    }

    public static boolean checkSpecialCharacters(String str) {
        return getSpecialCharacters(str).size() > 0;
    }

    public static List<String> getSpecialCharacters(String str) {
        return Arrays.stream(str.split(""))
                .filter(s -> !s.matches("[a-zA-Z0-9]*"))
                .collect(Collectors.toList());
    }

    public static String filterSpecialCharacters(String str) {
        return Arrays.stream(str.split(""))
                .filter(s -> s.matches("[a-zA-Z0-9]*"))
                .collect(Collectors.joining());
    }

    public static String capitalize(String str, CapitalizeMode mode) {
        String[] split = str.split("[\\s!@#$%^&*()=+-_,]+");
        StringBuilder sb = new StringBuilder();
        for (String s : split) {
            switch (mode) {
                case FIRST:
                    sb.append(s.substring(0, 1).toUpperCase());
                    if (s.length() > 1)
                        sb.append(s.substring(1).toLowerCase());
                    break;
                case ALL:
                    sb.append(s.toUpperCase());
                    break;
            }
        }
        return sb.toString();
    }

    public static String format(Material material) {
        StringBuilder sb = new StringBuilder();
        String[] name = material.name().split("_");
        for (String s : name) {
            sb.append(s.charAt(0)).append(s.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    public static String format(Location location) {
        return format(location, true);
    }

    public static String format(Location location, boolean includeBrackets) {
        return (includeBrackets ? "[" : "")
                + (location.getWorld() == null ? "none" : location.getWorld().getName()) + ", " +
                location.getBlockX() + ", " +
                location.getBlockY() + ", " +
                location.getBlockZ() + "" +
                (includeBrackets ? "]" : "");
    }

    public static enum CapitalizeMode {
        FIRST,
        ALL;
    }

}
