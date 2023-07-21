package com.github.tezvn.starpvp.api;

import com.google.common.collect.Lists;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractDatabase {

    private final Plugin plugin;

    private final String username;

    private String password;

    private final String name;

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

        private final HikariConfig hikariConfig;

        private HikariDataSource dataSource;

        public DeprecatedDatabase(Plugin plugin, String username, String password, String name, String host, String port, int poolSize, int timeout, int idleTimeout, int lifeTime) {
            super(plugin, username, password, name, host);
            this.port = port;

            hikariConfig = new HikariConfig();
            String url = "jdbc:mysql://" + getHost() + (getPort().equalsIgnoreCase("default") ? ":3306" : ":" + getPort()) + "/" + getName();
            hikariConfig.setJdbcUrl(url);
            hikariConfig.setUsername(getUsername());
            hikariConfig.setPassword(getPassword());
            hikariConfig.setMaximumPoolSize(poolSize);
            hikariConfig.setConnectionTimeout(timeout);
            hikariConfig.setIdleTimeout(idleTimeout);
            hikariConfig.setMaxLifetime(lifeTime);

            connect();
        }

        public HikariDataSource getDataSource(){
            return dataSource;
        }

        public String getPort() {
            return this.port;
        }

        public Connection getConnection() throws SQLException {
            return dataSource.getConnection();
        }

        public void connect() {
            try {
                getPlugin().getLogger().info("Connecting to database...");

                dataSource = new HikariDataSource(hikariConfig);

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
                try (Connection connection = getConnection()){
                    connection.createStatement().executeUpdate("CREATE DATABASE " + databaseName);
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        public boolean hasDatabase(String databaseName) {
            try (Connection connection = getConnection()){
                ResultSet set = connection.getMetaData().getCatalogs();
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
                if (!isConnected())
                    return false;
                StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS `" + name + "` (");
                for (DatabaseElement element : elements) {
                    StringBuilder typeBuilder = new StringBuilder(element.getType().name().replace("_", ""));
                    if (element.getType() == DatabaseElement.Type.VAR_CHAR)
                        typeBuilder.append("(255)");
                    builder.append("`").append(element.getName()).append("` ")
                            .append(typeBuilder).append(" NULL, ");
                }
                builder.delete(builder.length() - 2, builder.length());
                builder.append(");");
                Connection connection = getConnection();
                connection.createStatement().executeUpdate(builder.toString());
                connection.close();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        public List<String> getTables() {
            try {
                ResultSet rs = getConnection().getMetaData().getTables(null, null, "%", null);
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
            try (Connection connection = getConnection()){
                String query = "SELECT `" + toFind + "` FROM `" + table + "` WHERE `" + key + "`='" + value + "'";

                return connection.createStatement().executeQuery(query).next();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        public boolean add(String table, DatabaseInsertion... insertions) {
            try (Connection connection = getConnection()){
                StringBuilder query = new StringBuilder("INSERT INTO `" + table + "` (");
                String queryKey = Arrays.stream(insertions).map(i -> "`" + i.getKey() + "`").collect(Collectors.joining(", "));
                String queryValue = Arrays.stream(insertions).map(i -> "'" + i.getValue() + "'").collect(Collectors.joining(", "));
                query.append(queryKey).append(") VALUES (").append(queryValue).append(")");
                connection.prepareStatement(query.toString()).executeUpdate();
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
            try (Connection connection = getConnection()){
                String query = "DELETE FROM `" + table + "` WHERE `" + key + "`='" + value + "'";
                connection.createStatement().executeUpdate(query);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        public boolean update(String table, DatabaseInsertion toUpdate, DatabaseInsertion... insertions) {
            try (Connection connection = getConnection()){
                StringBuilder query = new StringBuilder("UPDATE `" + table + "` SET ");
                for (DatabaseInsertion e : insertions)
                    query.append("`").append(e.getKey()).append("`='").append(e.getValue()).append("', ");
                query.delete(query.length() - 2, query.length()).append(" ");
                query.append("WHERE `").append(toUpdate.getKey()).append("`='").append(toUpdate.getValue()).append("';");
                connection.createStatement().executeUpdate(query.toString());
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

    }

    public static class MySQL extends DeprecatedDatabase {
        public MySQL(Plugin plugin, String username, String password, String name, String host, String port, int poolSize, int timeout, int idleTimeout, int lifeTime) {
            super(plugin, username, password, name, host, port, poolSize, timeout, idleTimeout, lifeTime);
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
