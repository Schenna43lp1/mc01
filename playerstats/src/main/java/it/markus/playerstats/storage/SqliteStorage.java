package it.markus.playerstats.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * SQLite-Backend: lokale Datei plugins/PlayerStats/playerstats.db.
 */
public final class SqliteStorage extends JdbcStorage {

    private final String url;

    public SqliteStorage(File dataFolder, String tablePrefix, Logger log) {
        super(tablePrefix, log);
        this.url = "jdbc:sqlite:" + new File(dataFolder, "playerstats.db").getAbsolutePath();
    }

    @Override
    protected Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }

    @Override
    protected String driverClass() {
        return "org.sqlite.JDBC";
    }

    @Override
    protected String upsertSql() {
        return "INSERT INTO " + table + " (uuid, stat_key, value) VALUES (?, ?, ?) "
                + "ON CONFLICT(uuid, stat_key) DO UPDATE SET value = excluded.value";
    }

    @Override
    public String name() {
        return "SQLite";
    }
}
