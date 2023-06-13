package com.github.tezvn.starpvp.api;

import com.google.common.collect.Lists;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.bukkit.plugin.Plugin;

public abstract class AbstractDatabase {

    private Plugin plugin;

    private String username;

    private String password;

    private String name;

    private String host;

    private boolean connected = false;

    public AbstractDatabase(Plugin plugin, String username, String password, String name, String host) {
        this.plugin = plugin;
        this.username = username;
        if (password.length() > 0)
            this.password = password;
        this.name = name;
        this.host = host;
    }

    public Plugin getPlugin() {
        return this.plugin;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getName() {
        return this.name;
    }

    public String getHost() {
        return this.host;
    }

    public boolean isConnected() {
        return this.connected;
    }

    protected void setConnected(boolean connected) {
        this.connected = connected;
    }

    protected abstract void connect();

    public static abstract class DeprecatedDatabase extends AbstractDatabase {
        private final String port;

        private Connection connection;

        public DeprecatedDatabase(Plugin plugin, String username, String password, String name, String host, String port) {
            super(plugin, username, password, name, host);
            this.port = port;
            connect();
        }

        public String getPort() {
            return this.port;
        }

        public Connection getConnection() {
            return this.connection;
        }

        public void connect() {
            try {
                getPlugin().getLogger().info("Connecting to database...");
                String url = "jdbc:mysql://" + getHost() + (getPort().equalsIgnoreCase("default") ? ":3306" : ":" + getPort()) + "/" + getName();
                this.connection = DriverManager.getConnection(url, getUsername(), getPassword());
                getPlugin().getLogger().info("Connected to database success!");
                setConnected(true);
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

        public boolean createTable(String name, DatabaseElement... elements) {
            try {
                if (!isConnected() || hasTable(name))
                    return false;
                StringBuilder builder = new StringBuilder("CREATE TABLE `" + name + "` (");
                for (DatabaseElement element : elements) {
                    StringBuilder typeBuilder = new StringBuilder(element.getType().name().replace("_", ""));
                    if (element.getType() == DatabaseElement.Type.VAR_CHAR)
                        typeBuilder.append("(255)");
                    builder.append("`").append(element.getName()).append("` ")
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

        public boolean add(String table, DatabaseInsertion... insertions) {
            try {
                StringBuilder query = new StringBuilder("INSERT INTO `" + table + "`(");
                StringBuilder queryKey = new StringBuilder();
                StringBuilder queryValue = new StringBuilder();
                for (DatabaseInsertion insertion : insertions) {
                    queryKey.append(insertion.getKey()).append(", ");
                    queryValue.append("?, ");
                }
                queryKey = new StringBuilder(queryKey.substring(0, queryKey.length() - 2));
                queryValue = new StringBuilder(queryValue.substring(0, queryValue.length() - 2));
                query.append(queryKey).append(") VALUES (").append(queryValue).append(")");
                PreparedStatement statement = this.connection.prepareStatement(query.toString());
                for (int i = 0; i < insertions.length; i++) {
                    DatabaseInsertion e = insertions[i];
                    statement.setObject(i + 1, e.getValue());
                }
                statement.executeUpdate();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        public boolean addOrUpdate(String table, DatabaseInsertion check, DatabaseInsertion... insertions) {
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

        public boolean update(String table, DatabaseInsertion toUpdate, DatabaseInsertion... insertions) {
            try {
                StringBuilder query = new StringBuilder("UPDATE `" + table + "` SET ");
                for (DatabaseInsertion e : insertions)
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
                PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM " + table);
                return statement.executeQuery();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

    }

    public static class MySQL extends DeprecatedDatabase {
        public MySQL(Plugin plugin, String username, String password, String name, String host, String port) {
            super(plugin, username, password, name, host, port);
        }
    }

    public static class DatabaseInsertion {
        private String key;

        private Object value;

        public DatabaseInsertion(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return this.key;
        }

        public Object getValue() {
            return this.value;
        }
    }

    public static class DatabaseElement {
        private final String name;

        private final Type synchronizeType;

        public DatabaseElement(String name, Type synchronizeType) {
            this.name = name;
            this.synchronizeType = synchronizeType;
        }

        public String getName() {
            return this.name;
        }

        public Type getType() {
            return this.synchronizeType;
        }

        public enum Type {
            CHAR, VAR_CHAR, BINARY, VAR_BINARY, LONG_TEXT, BIT, TINY_INT, BOOL, BOOLEAN, SMALL_INT, MEDIUM_INT, INT, INTEGER, BIG_INT, FLOAT, DOUBLE, DECIMAL;
        }
    }
}
