package com.github.tezvn.starpvp.core.player;

import com.cryptomorin.xseries.XSound;
import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.PlayerManager;
import com.github.tezvn.starpvp.api.player.PlayerStatistic;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.api.player.cooldown.Cooldown;
import com.github.tezvn.starpvp.api.rank.RankPenalty;
import com.github.tezvn.starpvp.api.rank.SPRank;
import com.github.tezvn.starpvp.core.player.PlayerManagerImpl;
import com.github.tezvn.starpvp.core.player.SPPlayerImpl;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import com.github.tezvn.starpvp.core.utils.time.TimeUnits;
import com.github.tezvn.starpvp.core.utils.time.TimeUtils;
import dev.dejvokep.boostedyaml.YamlDocument;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalField;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class PlayerHandler extends BukkitRunnable {

    private final PlayerManager playerManager;

    private final SPPlugin plugin;

    private final YamlDocument config;

    private long time;

    public PlayerHandler(PlayerManager playerManager) {
        this.playerManager = playerManager;
        this.plugin = ((PlayerManagerImpl) playerManager).getPlugin();
        this.config = plugin.getDocument();
        runTaskTimerAsynchronously(plugin, 0, 20);
    }

    @Override
    public void run() {
        long now = TimeUtils.newInstance().getNewTime();
        Bukkit.getOnlinePlayers().forEach(player -> {
            SPPlayer spPlayer = playerManager.getPlayer(player);
            if (spPlayer == null)
                return;
            if(new DateTime(now).compare(new DateTime(time)))
                spPlayer.setStatistic(PlayerStatistic.COMBAT_LOGOUT_TIMES, 0L);

            SPRank rank = spPlayer.getRank();
            long combatTime = spPlayer.getStatistic(PlayerStatistic.COMBAT_TIMESTAMP);
            RankPenalty penalty = rank.getPenalty();
            if(penalty != null) {
                long lastCombat = spPlayer.getStatistic(PlayerStatistic.LAST_COMBAT_TIME);
                long lastPenalty = spPlayer.getStatistic(PlayerStatistic.LAST_PENALTY_TIME);
                if(lastPenalty > 0 && lastCombat > 0) {
                    int activeDays = penalty.getActiveDays();
                    long date = TimeUtils.of(lastCombat).add(TimeUnits.DAY, activeDays).getNewTime();
                    if (date < now) {
                        long periodDate = TimeUtils.of(lastPenalty).add(TimeUnits.DAY, penalty.getPeriod()).getNewTime();
                        if (periodDate < now) {
                            spPlayer.subtractEloPoint(penalty.getEloLost());
                            spPlayer.setStatistic(PlayerStatistic.LAST_PENALTY_TIME, now);
                        }
                        return;
                    }
                }
            }
            if (combatTime != 0) {
                String duration = config == null ? "30s" : config.getString("cooldown.exit-combat", "30s");
                long offset = TimeUtils.of(combatTime).add(duration).getNewTime() - now;
                int secondLeft = toSecond(offset);
                if(secondLeft > 0) {
                    if(secondLeft <= 5) {
                        MessageUtils.sendTitle(player,
                                "&6&l" + secondLeft + " giây",
                                "&7Sắp thoát trạng thái giao tranh...");
                    }
                    return;
                }
                spPlayer.setStatistic(PlayerStatistic.COMBAT_TIMESTAMP, 0);
                MessageUtils.sendTitle(player, "&a&l✔", "&7Bạn đã thoát giao tranh");
                XSound.ENTITY_PLAYER_LEVELUP.play(player);
                getPlayerManager().saveToDatabase(player);
                return;
            }
            Cooldown cooldown = spPlayer.getCooldown();
            if(cooldown != null) {
                long offset = cooldown.getEndTime() - now;
                int secondLeft = toSecond(offset);
                if(secondLeft > 0) {
                    if(secondLeft <= 5) {
                        MessageUtils.sendTitle(player,
                                "&6&l" + secondLeft + " giây",
                                "&7Sắp được tham gia giao tranh lại");
                    }
                    return;
                }
                spPlayer.removeCooldown();
                XSound.ENTITY_PLAYER_LEVELUP.play(player);
                getPlayerManager().saveToDatabase(player);
            }
        });
        this.time = now;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    private int toSecond(long millis) {
        return (int) millis / 1000;
    }

}
