package com.github.tezvn.starpvp.api.player;

import com.github.tezvn.starpvp.api.player.cooldown.Cooldown;
import com.github.tezvn.starpvp.api.rank.SPRank;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public interface SPPlayer {

    UUID getUniqueId();

    String getPlayerName();

    OfflinePlayer asOfflinePlayer();

    Player asPlayer();

    long getEloPoint();

    void setEloPoint(long eloPoint);

    void addEloPoint(long eloPoint);

    void subtractEloPoint(long eloPoint);

    SPRank getRank();

    long getStatistic(PlayerStatistic statistic);

    void setStatistic(PlayerStatistic statistic, long value);

    <T extends Cooldown> T getCooldown();

    <T extends Cooldown> void setCooldown(T cooldown);

    void removeCooldown();

    Map<UUID, Long> getKillsCooldown();

    long getKillCooldown(OfflinePlayer player);

    void addKillCooldown(OfflinePlayer player);

    void removeKillCooldown(OfflinePlayer player);

    Map<String, Object> serialize();

}
