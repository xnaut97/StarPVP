package com.github.tezvn.starpvp.api;

import com.github.tezvn.starpvp.api.AbstractDatabase.MySQL;
import com.github.tezvn.starpvp.api.player.PlayerCache;
import com.github.tezvn.starpvp.api.player.PlayerManager;
import com.github.tezvn.starpvp.api.rank.RankManager;
import dev.dejvokep.boostedyaml.YamlDocument;
import org.bukkit.plugin.Plugin;

public interface SPPlugin extends Plugin {

    YamlDocument getDocument();

    PlayerManager getPlayerManager();

    RankManager getRankManager();

    MySQL getDatabase();

    PlayerCache getPlayerCache();

    void reload();

}
