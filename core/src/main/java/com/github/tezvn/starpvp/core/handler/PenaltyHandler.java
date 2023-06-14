package com.github.tezvn.starpvp.core.handler;

import com.github.tezvn.starpvp.api.player.PlayerManager;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import com.github.tezvn.starpvp.core.utils.time.TimeUtils;
import org.bukkit.entity.Player;

public class PenaltyHandler extends AbstractHandler<Long> {

    public PenaltyHandler(PlayerManager playerManager) {
        super(playerManager, Type.PENALTY);
    }

    @Override
    public long getEndTime(Long value) {
        return TimeUtils.of(value).getNewTime();
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
