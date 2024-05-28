package com.github.tezvn.starpvp.core.player;

import com.cryptomorin.xseries.XSound;
import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.PlayerManager;
import com.github.tezvn.starpvp.api.player.PlayerStatistic;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.api.player.cooldown.Cooldown;
import com.github.tezvn.starpvp.api.rank.RankPenalty;
import com.github.tezvn.starpvp.api.rank.SPRank;
import com.github.tezvn.starpvp.core.SPPluginImpl;
import com.github.tezvn.starpvp.core.log.LogType;
import com.github.tezvn.starpvp.core.log.PenaltyLog;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import com.github.tezvn.starpvp.core.utils.time.TimeUnits;
import com.github.tezvn.starpvp.core.utils.time.TimeUtils;
import dev.dejvokep.boostedyaml.YamlDocument;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;

public class PlayerHandler extends BukkitRunnable {

    private final PlayerManager playerManager;

    private final SPPlugin plugin;

    private final YamlDocument config;

    private long time;

    public PlayerHandler(PlayerManager playerManager) {
        this.playerManager = playerManager;
        this.plugin = ((PlayerManagerImpl) playerManager).getPlugin();
        this.config = plugin.getDocument();
        runTaskTimerAsynchronously(plugin, 20, 20);
    }

    @Override
    public void run() {
        long now = TimeUtils.newInstance().getNewTime();
        getPlayerManager().getPlayers().forEach(spPlayer -> {
            OfflinePlayer offlinePlayer = spPlayer.asOfflinePlayer();
            if (new DateTime(TimeUtils.newInstance().getNewTime()).isAfter(new DateTime(time)))
                spPlayer.setStatistic(PlayerStatistic.COMBAT_LOGOUT_TIMES, 0);

            // Nếu người chơi đang trong combat
            long combatTime = spPlayer.getStatistic(PlayerStatistic.COMBAT_TIMESTAMP);
            if (combatTime != 0) {
                String duration = config == null ? "30s" : config.getString("cooldown.exit-combat", "30s");
                long offset = TimeUtils.of(combatTime).add(duration).getNewTime() - now;
                int secondLeft = toSecond(offset);
                if (secondLeft > 0) {
                    if (secondLeft <= 5)
                        if (offlinePlayer.getPlayer() != null)
                            MessageUtils.sendTitle(offlinePlayer.getPlayer(),
                                    "&6&l" + secondLeft + " giây",
                                    "&7Sắp thoát trạng thái giao tranh...");
                    return;
                }
                // Người chơi thoát giao tranh
                spPlayer.setStatistic(PlayerStatistic.COMBAT_TIMESTAMP, 0);
                if (offlinePlayer.getPlayer() != null) {
                    MessageUtils.sendTitle(offlinePlayer.getPlayer(), "&a&l✔", "&7Bạn đã thoát giao tranh");
                    XSound.ENTITY_PLAYER_LEVELUP.play(offlinePlayer.getPlayer());
                }
                getPlayerManager().saveToDatabase(offlinePlayer);
            }

            Cooldown cooldown = spPlayer.getCooldown();
            if (cooldown != null) {
                long offset = cooldown.getEndTime() - now;
                int secondLeft = toSecond(offset);
                if (secondLeft > 0) {
                    if (secondLeft <= 5)
                        if (offlinePlayer.getPlayer() != null)
                            MessageUtils.sendTitle(offlinePlayer.getPlayer(),
                                    "&6&l" + secondLeft + " giây",
                                    "&7Sắp được tham gia giao tranh lại");
                    return;
                }
                // Xóa cdr
                spPlayer.removeCooldown();
                if (offlinePlayer.getPlayer() != null)
                    XSound.ENTITY_PLAYER_LEVELUP.play(offlinePlayer.getPlayer());
                getPlayerManager().saveToDatabase(offlinePlayer);
            }

            SPRank rank = spPlayer.getRank();
            RankPenalty penalty = rank.getPenalty();
            if (penalty != null) {
                // Nếu rank có mức phạt vì inactive quá lâu
                long lastCombat = spPlayer.getStatistic(PlayerStatistic.LAST_COMBAT_TIME);
                if (lastCombat > 0) {
                    int activeDays = penalty.getActiveDays();
                    long date = TimeUtils.of(lastCombat).add(TimeUnits.DAY, activeDays).getNewTime();
                    if (date < TimeUtils.newInstance().getNewTime()) {
                        // Kích hoạt trừ điểm nếu off quá số ngày
                        long lastPenalty = spPlayer.getStatistic(PlayerStatistic.LAST_PENALTY_TIME);
                        if(lastPenalty > 0) {
                            long periodDate = TimeUtils.of(lastPenalty).add(TimeUnits.DAY, penalty.getPeriod()).getNewTime();
                            if (periodDate > TimeUtils.newInstance().getNewTime()) return; //Chưa tới hạn trừ điểm theo định kì X ngày
                        }
                        long oldElo = spPlayer.getEloPoint();
                        long newElo = oldElo - penalty.getEloLost();
                        spPlayer.subtractEloPoint(penalty.getEloLost());
                        spPlayer.setStatistic(PlayerStatistic.LAST_PENALTY_TIME, TimeUtils.newInstance().getNewTime());
                        getPlayerManager().saveToDatabase(offlinePlayer);
                        PenaltyLog log = ((SPPluginImpl) plugin).getLog(LogType.PENALTY);
                        log.write(offlinePlayer.getName() + "(" + oldElo + " -> " + newElo + ") (-" + penalty.getEloLost() + ")");
                        if(offlinePlayer.isOnline())
                            MessageUtils.sendMessage(offlinePlayer.getPlayer(), "&cBạn bị trừ &6" + penalty.getEloLost() + " &cvì không có hoạt động giao tranh nào gần đây.");
                        else
                            ((PlayerManagerImpl) playerManager).updateEloLost(offlinePlayer, penalty.getEloLost());
                    }
                }
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
