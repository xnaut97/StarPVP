package com.github.tezvn.starpvp.core;

import com.github.tezvn.starpvp.api.AbstractDatabase;
import com.github.tezvn.starpvp.api.AbstractDatabase.MySQL;
import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.PlayerManager;
import com.github.tezvn.starpvp.core.player.PlayerManagerImpl;
import com.github.tezvn.starpvp.core.utils.BaseMenu;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class SPPluginImpl extends JavaPlugin implements SPPlugin {

    private YamlDocument document;

    private PlayerManager playerManager;

    private MySQL database;

    @Override
    public void onEnable() {
        BaseMenu.register(this);
        setupConfig();
        setupDatabase();
        this.playerManager = new PlayerManagerImpl(this);
    }

    @Override
    public void onDisable() {
        BaseMenu.forceCloseAll();
    }

    @Override
    public YamlDocument getDocument() {
        return document;
    }

    @Override
    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    private void setupConfig() {
        try {
            this.document = YamlDocument.create(new File("config.yml"),
                    getResource("config.yml"),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version"))
                            .build());
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupDatabase() {

    }
}
