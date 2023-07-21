package com.github.tezvn.starpvp.api;

import com.google.common.collect.Lists;
import org.bukkit.plugin.Plugin;

import java.sql.*;
import java.util.List;

public final class Database {

    private final Plugin plugin;

    private final String username;

    private final String password;

    private final String name;

    private final String host;

    private final String port;

    private Connection connection;

    private Database(Plugin plugin, String username, String password, String name, String host, String port) {
        this.plugin = plugin;
        this.username = username;
        this.password = password;
        this.name = name;
        this.host = host;
        this.port = port;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public boolean isConnected() {
        return this.connection != null;
    }

    public void connect() {
        try {
            getPlugin().getLogger().info("Connecting to database...");
            String url = "jdbc:mysql://" + getHost() + (getPort().equalsIgnoreCase("default") ? ":3306" : ":" + getPort()) + "/" + getName();
            this.connection = DriverManager.getConnection(url, getUsername(), getPassword());
            getPlugin().getLogger().info("Connected to database success!");
        } catch (Exception e) {
            getPlugin().getLogger().severe("Couldn't connect to database!");
        }
    }

    public boolean createDatabase(String databaseName) {
        try {
            if (hasDatabase(databaseName))
                return false;
            this.connection.createStatement().executeUpdate("CREATE DATABASE " + databaseName);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasDatabase(String databaseName) {
        try {
            ResultSet set = this.connection.getMetaData().getCatalogs();
            boolean exist = false;
            while (set.next()) {
                String name = set.getString(1);
                if (name.equals(databaseName)) {
                    exist = true;
                    break;
                }
            }
            return exist;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean createTable(String name, Database.Row... elements) {
        try {
            if (!isConnected() || hasTable(name))
                return false;
            StringBuilder builder = new StringBuilder("CREATE TABLE `" + name + "` (");
            for (Database.Row element : elements) {
                StringBuilder typeBuilder = new StringBuilder(element.getType().name().replace("_", ""));
                if (element.getType() == Database.Row.Type.VAR_CHAR)
                    typeBuilder.append("(255)");
                builder.append("`").append(element.getKey()).append("` ")
                        .append(typeBuilder).append(" NULL, ");
            }
            builder.delete(builder.length() - 2, builder.length());
            builder.append(");");
            this.connection.createStatement().executeUpdate(builder.toString());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasTable(String name) {
        if (this.connection == null)
            return false;
        try {
            return this.connection.getMetaData().getTables(null, null, name, null).next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String> getTables() {
        try {
            ResultSet rs = this.connection.getMetaData().getTables(null, null, "%", null);
            List<String> list = Lists.newArrayList();
            while (rs.next())
                list.add(rs.getString(3));
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean has(String table, String toFind, String key, Object value) {
        try {
            String query = "SELECT `" + toFind + "` FROM `" + table + "` WHERE `" + key + "`='" + value + "'";
            return this.connection.createStatement().executeQuery(query).next();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean add(String table, Entry... insertions) {
        try {
            StringBuilder query = new StringBuilder("INSERT INTO `" + table + "`(");
            StringBuilder queryKey = new StringBuilder();
            StringBuilder queryValue = new StringBuilder();
            for (Entry insertion : insertions) {
                queryKey.append(insertion.getKey()).append(", ");
                queryValue.append("?, ");
            }
            queryKey = new StringBuilder(queryKey.substring(0, queryKey.length() - 2));
            queryValue = new StringBuilder(queryValue.substring(0, queryValue.length() - 2));
            query.append(queryKey).append(") VALUES (").append(queryValue).append(")");
            PreparedStatement statement = this.connection.prepareStatement(query.toString());
            for (int i = 0; i < insertions.length; i++) {
                Entry e = insertions[i];
                statement.setObject(i + 1, e.getValue());
            }
            statement.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean addOrUpdate(String table, Entry check, Entry... insertions) {
        if (check != null) {
            boolean found = has(table, check.getKey(), check.getKey(), check.getValue());
            if (found)
                return update(table, check, insertions);
        }
        return add(table, insertions);
    }

    public boolean remove(String table, String key, Object value) {
        try {
            String query = "DELETE FROM `" + table + "` WHERE `" + key + "`='" + value + "'";
            this.connection.createStatement().executeUpdate(query);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean update(String table, Entry toUpdate, Entry... insertions) {
        try {
            StringBuilder query = new StringBuilder("UPDATE `" + table + "` SET ");
            for (Entry e : insertions)
                query.append("`").append(e.getKey()).append("`='").append(e.getValue()).append("', ");
            query.delete(query.length() - 2, query.length()).append(" ");
            query.append("WHERE `").append(toUpdate.getKey()).append("`='").append(toUpdate.getValue()).append("'");
            this.connection.createStatement().executeUpdate(query.toString());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public ResultSet getData(String table) {
        if (!isConnected())
            return null;
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table);
            return statement.executeQuery();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ResultSet find(String table, String key, Object value) {
        if (!isConnected())
            return null;
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM " + table + " WHERE " + key + "=?");
            if(value instanceof String)
                statement.setString(1, String.valueOf(value));
            return statement.executeQuery();
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static final class Entry extends BaseRow {

        private final Object value;

        public Entry(String key, Object value) {
            super(key);
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

    }

    public static final class Row extends BaseRow {

        private final Type type;

        public Row(String key, Type type) {
            super(key);
            this.type = type;
        }

        public Type getType() {
            return this.type;
        }

        public enum Type {
            CHAR, VAR_CHAR, BINARY, VAR_BINARY, LONG_TEXT, BIT, TINY_INT, BOOL, BOOLEAN, SMALL_INT, MEDIUM_INT, INT, INTEGER, BIG_INT, FLOAT, DOUBLE, DECIMAL;
        }
    }

    protected abstract static class BaseRow {
        private final String key;

        public BaseRow(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private static class Builder {

        private Plugin plugin;

        private String username;

        private String password;

        private String name;

        private String host;

        private String port;

        public Plugin getPlugin() {
            return plugin;
        }

        public String getUsername() {
            return username;
        }   

        public String getPassword() {
            return password;
        }

        public String getName() {
            return name;
        }

        public String getHost() {
            return host;
        }

        public String getPort() {
            return port;
        }

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setHost(String host) {
            this.host = host;
            return this;
        }

        public Builder setPort(String port) {
            this.port = port;
            return this;
        }

        public Database build(Plugin plugin) {
            return new Database(plugin, getUsername(), getPassword(), getName(), getHost(), getPort());
        }
    }
}
