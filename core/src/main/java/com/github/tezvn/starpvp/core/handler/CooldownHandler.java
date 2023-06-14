package com.github.tezvn.starpvp.core.handler;

import com.cryptomorin.xseries.XSound;
import com.github.tezvn.starpvp.api.player.CombatCooldown;
import com.github.tezvn.starpvp.api.player.PlayerManager;
import com.github.tezvn.starpvp.core.player.PlayerManagerImpl;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import com.github.tezvn.starpvp.core.utils.time.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class CooldownHandler extends AbstractHandler<CombatCooldown> {

    public CooldownHandler(PlayerManager playerManager) {
        super(playerManager, Type.COOLDOWN);
    }

    @Override
    public long getEndTime(CombatCooldown value) {
        return TimeUtils.of(value.getStartTime()).setNewTime(value.getEndTime()).getNewTime();
    }

    @Override
    public void onAboutToRemove(Player player, int secondsLeft) {
        MessageUtils.sendTitle(player,
                "&6&l" + secondsLeft + " giây",
                "&7Sắp được tham gia giao tranh lại");
    }

    @Override
    public void onRemoved(Player player) {
        MessageUtils.sendTitle(player, "&a&l✔", "&7Bạn có thể tham gia giao tranh trở lại.");
    }
}