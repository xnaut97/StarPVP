package com.github.tezvn.starpvp.core.log;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.core.SPPluginImpl;
import com.github.tezvn.starpvp.core.utils.time.TimeUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

public abstract class BaseLog {

    private final Plugin plugin;

    private int attempts;

    private File folder;

    private File latest;

    private final LogType type;

    public BaseLog(Plugin plugin, LogType type) {
        this.plugin = plugin;
        this.type = type;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public LogType getType() {
        return type;
    }

    public void write(String... lines) {
        try {
            File file = getFile();
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            Arrays.stream(lines).forEach(str -> config.set("[" + getFullFormat() + "]", str));
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File getFile() throws Exception {
        SPPlugin plugin = ((SPPlugin) getPlugin());
        File folder = getFolder();
        File file = getLatest();
        long maxSize = plugin.getDocument().getInt("log.max-file-size", 10);
        long megabytes = file.length() / 1024 / 1024;
        if (megabytes >= maxSize) {
            File archive = new File(folder + "/" + getDate());
            if (!archive.exists())
                archive.mkdirs();
            long count = Arrays.stream(Objects.requireNonNull(folder.listFiles()))
                    .filter(f -> f.getName().endsWith(".log") && f.getName().startsWith(getDate())).count();
            boolean rename = file.renameTo(new File(archive + "/" + getDate() + (count > 0 ? "-" + count : "")));
            if (attempts > 3)
                return file;
            if (!rename) {
                if (attempts == 3)
                    plugin.getLogger().severe("Attempted to rename log file but failed, continue to use latest.log");
                else
                    plugin.getLogger().severe("Failed to rename log file, attempting to rename...");
                attempts++;
                return getFile();
            }
            plugin.getLogger().warning("Log file reached limit size, archiving to folder " + folder + "/" + getDate());
            File newFile = new File(folder + "/latest.log");
            if (!newFile.exists()) {
                boolean create = newFile.createNewFile();
                if (create) plugin.getLogger().warning("Created new log file 'lastest.log'");
            }
            return newFile;
        }
        return file;
    }

    private String getFullFormat() {
        return TimeUtils.newInstance().format();
    }

    private String getDate() {
        return TimeUtils.newInstance().format("dd-MM-yyyy");
    }

    private File getFolder() {
        if (folder == null) {
            File folder = new File(plugin.getDataFolder() + "/logs/" + type.name().toLowerCase());
            boolean mkdirs = folder.mkdirs();
            if (mkdirs) plugin.getLogger().warning("Created '" + type.name().toLowerCase() + "' logs folder!");
            this.folder = folder;
        }
        return this.folder;
    }

    private File getLatest() throws Exception {
        if (this.latest == null) {
            File file = new File(folder + "/latest.log");
            if (!file.exists()) {
                boolean create = file.createNewFile();
                if (create) plugin.getLogger().warning("Created new '" + type.name().toLowerCase() + "'latest log file!");
            }
            this.latest = file;
        }
        return this.latest;
    }

}
