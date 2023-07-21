package com.github.tezvn.starpvp.core.player;

import com.cryptomorin.xseries.XSound;
import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.PlayerManager;
import com.github.tezvn.starpvp.api.player.PlayerStatistic;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.api.player.cooldown.Cooldown;
import com.github.tezvn.starpvp.core.player.PlayerManagerImpl;
import com.github.tezvn.starpvp.core.player.SPPlayerImpl;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import com.github.tezvn.starpvp.core.utils.time.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerHandler extends BukkitRunnable {

    private final PlayerManager playerManager;
    private final SPPlugin plugin;

    @SuppressWarnings("unchecked")
    public PlayerHandler(PlayerManager playerManager) {
        this.playerManager = playerManager;
        this.plugin = ((PlayerManagerImpl) playerManager).getPlugin();
        runTaskTimerAsynchronously(plugin, 20, 20);
    }

    @Override
    public void run() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            SPPlayer spPlayer = playerManager.getPlayer(player);
            if (spPlayer == null)
                return;
            long now = TimeUtils.newInstance().getNewTime();
            long combatTime = spPlayer.getStatistic(PlayerStatistic.COMBAT_TIMESTAMP);
            if (combatTime != 0) {
                String duration = plugin.getDocument().getString("cooldown.exit-combat", "30s");
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
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    private int toSecond(long millis) {
        return (int) millis / 1000;
    }
}
