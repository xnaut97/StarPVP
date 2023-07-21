package com.github.tezvn.starpvp.core;

import com.github.tezvn.starpvp.api.AbstractDatabase.*;
import com.github.tezvn.starpvp.api.AbstractDatabase.MySQL;
import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.PlayerManager;
import com.github.tezvn.starpvp.api.rank.RankManager;
import com.github.tezvn.starpvp.core.commands.CommandManager;
import com.github.tezvn.starpvp.core.player.PlayerManagerImpl;
import com.github.tezvn.starpvp.core.rank.RankManagerImpl;
import com.github.tezvn.starpvp.core.utils.BaseMenu;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public class SPPluginImpl extends JavaPlugin implements SPPlugin {

    private YamlDocument document;

    private YamlDocument message;

    private PlayerManager playerManager;

    private MySQL database;
    private CommandManager commandManager;

    private RankManager rankManager;

    @Override
    public void onEnable() {
        BaseMenu.register(this);
        setupConfig();
        setupDatabase();
        this.rankManager = new RankManagerImpl(this);
        this.playerManager = new PlayerManagerImpl(this);
        this.commandManager = new CommandManager(this);
    }

    @Override
    public void onDisable() {
        BaseMenu.forceCloseAll();
        if(this.commandManager != null)
            this.commandManager.unregister();
    }

    @Override
    public YamlDocument getDocument() {
        return document;
    }

    @Override
    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    @Override
    public RankManager getRankManager() {
        return rankManager;
    }

    public MySQL getDatabase() {
        return database;
    }

    private void setupConfig() {
        try {
            this.document = YamlDocument.create(new File(getDataFolder(), "config.yml"),
                    Objects.requireNonNull(getResource("config.yml")),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version"))
                            .build());
            this.message = YamlDocument.create(new File(getDataFolder(), "messages.yml"),
                    Objects.requireNonNull(getResource("messages.yml")),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("message-version"))
                            .build());

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupDatabase() {
        boolean toggle = getConfig().getBoolean("database.toggle", true);
        if(!toggle)
            return;
        String username = getConfig().getString("database.username", "root");
        String password = getConfig().getString("database.password", "password");
        String name = getConfig().getString("database.name", "authenticator");
        String host = getConfig().getString("database.host", "localhost");
        String port = getConfig().getString("database.port", "3306");
        String tableName = getConfig().getString("database.table-name", "user");
        int poolSize = getConfig().getInt("database.pool.max-pool-size", 10);
        int timeout = getConfig().getInt("database.pool.timeout", 5000);
        int idleTimeout = getConfig().getInt("database.pool.idle-timeout", 600000);
        int lifeTime = getConfig().getInt("database.pool.max-life-time", 1800000);
        this.database = new MySQL(this, username, password, name, host, port, poolSize, timeout, idleTimeout, lifeTime);
        if(!this.database.isConnected()) {
            getLogger().info("Use local cache instead.");
            return;
        }
        boolean createResult = this.database.createTable(tableName,
                new DatabaseElement("uuid", DatabaseElement.Type.VAR_CHAR),
                new DatabaseElement("player_name", DatabaseElement.Type.VAR_CHAR),
                new DatabaseElement("rank", DatabaseElement.Type.VAR_CHAR),
                new DatabaseElement("elo", DatabaseElement.Type.VAR_CHAR),
                new DatabaseElement("statistic", DatabaseElement.Type.LONG_TEXT),
                new DatabaseElement("cooldown", DatabaseElement.Type.VAR_CHAR),
                new DatabaseElement("kills_cooldown", DatabaseElement.Type.LONG_TEXT));
        if (createResult)
            getLogger().info("Created table '" + tableName + "' success!");
    }
}
