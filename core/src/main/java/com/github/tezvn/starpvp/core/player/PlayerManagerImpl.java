package com.github.tezvn.starpvp.core.player;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.PlayerManager;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.google.common.collect.Maps;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerManagerImpl implements PlayerManager {

    private final Map<UUID, SPPlayer> players = Maps.newHashMap();

    private final SPPlugin plugin;

    public PlayerManagerImpl(SPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<SPPlayer> getPlayers() {
        return null;
    }

    @Override
    public SPPlayer getPlayer(OfflinePlayer player) {
        return null;
    }

    @Override
    public SPPlayer getPlayer(String name) {
        return null;
    }

    @Override
    public SPPlayer getPlayer(UUID uniqueId) {
        return null;
    }
}
