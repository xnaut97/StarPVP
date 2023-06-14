package com.github.tezvn.starpvp.core.handler;

import com.cryptomorin.xseries.XSound;
import com.github.tezvn.starpvp.api.player.PlayerManager;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.core.player.PlayerManagerImpl;
import com.github.tezvn.starpvp.core.player.SPPlayerImpl;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import com.github.tezvn.starpvp.core.utils.time.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class CombatHandler extends AbstractHandler<Long> {

    public CombatHandler(PlayerManager playerManager) {
        super(playerManager, Type.COMBAT);
    }

    @Override
    public long getEndTime(Long value) {
        return TimeUtils.of(value).add("30s").getNewTime();
    }

    @Override
    public void onAboutToRemove(Player player, int secondsLeft) {
        MessageUtils.sendTitle(player,
                "&6&l" + secondsLeft + " giây",
                "&7Sắp thoát trạng thái giao tranh...");
    }

    @Override
    public void onRemoved(Player player) {
        MessageUtils.sendTitle(player, "&a&l✔", "&7Bạn đã thoát giao tranh");
    }
}
