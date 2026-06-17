package it.markus.playerstats.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * MySQL-/MariaDB-Backend. Benoetigt einen erreichbaren DB-Server
 * (Zugangsdaten in der config.yml unter storage.mysql).
 */
public final class MySqlStorage extends JdbcStorage {

    private final String url;
    private final String user;
    private final String password;

    public MySqlStorage(String host, int port, String database, String user, String password,
                        String tablePrefix, Logger log) {
        super(tablePrefix, log);
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8";
        this.user = user;
        this.password = password;
    }

    @Override
    protected Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    protected String driverClass() {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    protected String upsertSql() {
        return "INSERT INTO " + table + " (uuid, stat_key, value) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE value = VALUES(value)";
    }

    @Override
    public String name() {
        return "MySQL";
    }
}
