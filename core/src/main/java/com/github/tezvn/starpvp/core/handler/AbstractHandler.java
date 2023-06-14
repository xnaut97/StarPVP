package com.github.tezvn.starpvp.core.handler;

import com.cryptomorin.xseries.XSound;
import com.github.tezvn.starpvp.api.player.PlayerManager;
import com.github.tezvn.starpvp.core.player.PlayerManagerImpl;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import com.github.tezvn.starpvp.core.utils.time.TimeUtils;
import com.google.common.collect.Maps;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AbstractHandler<T> extends BukkitRunnable {

    private final PlayerManager playerManager;

    private final Map<UUID, T> map;

    private final Type handlerType;

    @SuppressWarnings("unchecked")
    public AbstractHandler(PlayerManager playerManager, Type handlerType) {
        this.playerManager = playerManager;
        this.handlerType = handlerType;
        switch (this.handlerType) {
            case COOLDOWN -> {
                this.map = (Map<UUID, T>) ((PlayerManagerImpl) playerManager).getCombatCooldown();
            }
            case PENALTY -> {
                this.map = (Map<UUID, T>) ((PlayerManagerImpl) playerManager).getCombatPenalty();
            }
            case COMBAT -> {
                this.map = (Map<UUID, T>) ((PlayerManagerImpl) playerManager).getCombatTimestamp();
            }
            default -> this.map = Maps.newHashMap();
        }
        runTaskTimerAsynchronously(((PlayerManagerImpl) playerManager).getPlugin(), 20, 20);
    }

    @Override
    public void run() {
        Iterator<Map.Entry<UUID, T>> iterator = map.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<UUID, T> entry = iterator.next();
            UUID uuid = entry.getKey();
            T value = entry.getValue();
            Player player = Bukkit.getPlayer(uuid);
            long endTime = getEndTime(value);
            long now = TimeUtils.newInstance().getNewTime();
            if(endTime > now) {
                int secondsLeft = (int) ((endTime - now) / 1000);
                if(secondsLeft <= 5 && player != null) {
                    onAboutToRemove(player, secondsLeft);
                    XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(player, 1f,
                            ThreadLocalRandom.current().nextFloat(1, 2));
                }
                continue;
            }
            iterator.remove();
            if (player == null)
                continue;
            onRemoved(player);
            XSound.ENTITY_PLAYER_LEVELUP.play(player);
        }
    }

    public abstract long getEndTime(T value);

    public abstract void onAboutToRemove(Player player, int secondsLeft);

    public abstract void onRemoved(Player player);

    public enum Type {
        COOLDOWN,
        PENALTY,
        COMBAT,
    }
}
