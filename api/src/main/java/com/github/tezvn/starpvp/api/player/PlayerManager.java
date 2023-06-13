package com.github.tezvn.starpvp.api.player;

import com.github.tezvn.starpvp.api.player.SPPlayer;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.UUID;

public interface PlayerManager {

    List<SPPlayer> getPlayers();

    SPPlayer getPlayer(OfflinePlayer player);

    SPPlayer getPlayer(String name);

    SPPlayer getPlayer(UUID uniqueId);

}
