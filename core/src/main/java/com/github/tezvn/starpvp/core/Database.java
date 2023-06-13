package com.github.tezvn.starpvp.core;

import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.DriverManager;

public class Database {

    private final Plugin plugin;

    private String username;

    private String password;

    private String name;

    private String host;

    private String port;

    private Connection connection;

    public Database(Plugin plugin) {
        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public String getUsername() {
        return username;
    }

    public Database setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public Database setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getName() {
        return name;
    }

    public Database setName(String name) {
        this.name = name;
        return this;
    }

    public String getHost() {
        return host;
    }

    public Database setHost(String host) {
        this.host = host;
        return this;
    }

    public String getPort() {
        return port;
    }

    public Database setPort(String port) {
        this.port = port;
        return this;
    }

    public Connection getConnection() {
        return connection;
    }

    public Database connect() {
        try {
            getPlugin().getLogger().info("Connecting to database");
            String url = "jdbc:mysql://" + getHost() + (getPort().equalsIgnoreCase("default") ? ":3306" : ":" + getPort()) + "/" + getName();
            this.connection = DriverManager.getConnection(url, getUsername(), getPassword());
            getPlugin().getLogger().info("Connected to database!");
        } catch (Exception e) {
            getPlugin().getLogger().severe("Couldn't connect to database!");
        }
        return this;
    }

    public boolean isConnected() {
        return this.connection != null;
    }
    
}
