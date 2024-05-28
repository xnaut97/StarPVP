package com.github.tezvn.starpvp.core;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.PlayerStatistic;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class PluginExpansion extends PlaceholderExpansion {

    private final SPPlugin plugin;

    public PluginExpansion(SPPlugin plugin) {
        this.plugin = plugin;
        register();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "starpvp";
    }

    @Override
    public @NotNull String getAuthor() {
        return "TezVN";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String[] split = params.split("-");
        if (split.length > 1) {
            switch (split[0]) {
                case "player" -> {
                    switch (split[1]) {
                        case "rank" -> {
                            return plugin.getPlayerManager().getPlayer(player).getRank().getDisplayName();
                        }
                        case "elo" -> {
                            return String.valueOf(plugin.getPlayerManager().getPlayer(player).getEloPoint());
                        }
                        case "kill" -> {
                            return String.valueOf(plugin.getPlayerManager().getPlayer(player).getStatistic(PlayerStatistic.KILL_COUNT));
                        }
                        case "death" -> {
                            return String.valueOf(plugin.getPlayerManager().getPlayer(player).getStatistic(PlayerStatistic.DEATH_COUNT));
                        }
                        case "position" -> {
                            int position = getPosition(player);
                            return position == -1 ? "NaN" : String.valueOf(position);
                        }
                    }
                }
                case "leaderboard", "bxh" -> {
                    if (split[1].equals("top")) {
                        // leaderboard-top-{position}-{data_type}
                        if(split.length < 3) return null;

                        SPPlayer spPlayer = getPlayer(Integer.parseInt(split[2]));
                        if(spPlayer == null) return "";

                        if(split.length < 4) return null;
                        switch (split[3]) {
                            case "name" -> {
                                return spPlayer.getPlayerName();
                            }
                            case "elo" -> {
                                return String.valueOf(spPlayer.getEloPoint());
                            }
                            case "rank" -> {
                                return spPlayer.getRank().getDisplayName();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private SPPlayer getPlayer(int position) {
        List<SPPlayer> sorted = getSortedPlayers();
        return position < 1 || position > sorted.size() ? null : sorted.get(position-1);
    }

    private int getPosition(OfflinePlayer player) {
        List<SPPlayer> sorted = getSortedPlayers();
        for(int i = 0; i < sorted.size(); i++) {
            SPPlayer spPlayer = sorted.get(i);
            if(spPlayer.getPlayerName().equals(player.getName()))
                return i+1;
        }
        return -1;
    }

    private List<SPPlayer> getSortedPlayers() {
        return plugin.getPlayerManager().getPlayers().stream()
                .sorted(Comparator.comparing(SPPlayer::getEloPoint, Comparator.reverseOrder())).toList();
    }
}
