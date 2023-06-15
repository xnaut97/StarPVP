package com.github.tezvn.starpvp.api;

import com.github.tezvn.starpvp.api.AbstractDatabase.MySQL;
import com.github.tezvn.starpvp.api.player.PlayerManager;
import dev.dejvokep.boostedyaml.YamlDocument;
import org.bukkit.plugin.Plugin;

public interface SPPlugin extends Plugin {

    YamlDocument getDocument();

    PlayerManager getPlayerManager();

    MySQL getDatabase();

}
