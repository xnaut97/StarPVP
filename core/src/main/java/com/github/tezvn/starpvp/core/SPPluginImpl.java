package com.github.tezvn.starpvp.core;

import com.github.tezvn.starpvp.api.AbstractDatabase.*;
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

    public MySQL getDatabase() {
        return database;
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
        boolean toggle = getConfig().getBoolean("database.toggle", true);
        if (!toggle)
            return;
        String username = getDocument().getString("database.username", "root");
        String password = getDocument().getString("database.password", "");
        String name = getDocument().getString("database.name", "authenticator");
        String host = getDocument().getString("database.host", "localhost");
        String port = getDocument().getString("database.port", "3306");
        String tableName = getDocument().getString("database.table-name", "user");
        this.database = new MySQL(this, username, password, name, host, port);
        if (!this.database.isConnected()) {
            getLogger().info("Use local cache instead.");
            return;
        }
        boolean createResult = this.database.createTable(tableName,
                new DatabaseElement("uuid", DatabaseElement.Type.VAR_CHAR),
                new DatabaseElement("player_name", DatabaseElement.Type.VAR_CHAR),
                new DatabaseElement("data", DatabaseElement.Type.LONG_TEXT));
        if (createResult)
            getLogger().info("Created table '" + tableName + "' success!");
    }
}
