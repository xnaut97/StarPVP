package com.github.tezvn.starpvp.api;

import com.github.tezvn.starpvp.api.player.SPPlayer;
import org.bukkit.OfflinePlayer;

public interface PlayerManager {

    SPPlayer getPlayer(OfflinePlayer player);

}
