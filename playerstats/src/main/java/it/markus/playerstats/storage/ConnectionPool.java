package it.markus.playerstats.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimaler, threadsicherer JDBC-Connection-Pool – bewusst ohne externe
 * Bibliothek (HikariCP haette slf4j ins Shading gezogen), da das periodische
 * Batch-Schreiben kein ausgefeiltes Pooling braucht.
 *
 * Verbindungen werden lazy bis {@code maxSize} erzeugt, bei Rueckgabe auf
 * Gueltigkeit geprueft und ungueltige verworfen. {@link #borrow()} blockiert
 * kurz, falls der Pool ausgelastet ist.
 */
final class ConnectionPool implements AutoCloseable {

    @FunctionalInterface
    interface Factory {
        Connection open() throws SQLException;
    }

    private final Factory factory;
    private final int maxSize;
    private final BlockingQueue<Connection> idle;
    private final AtomicInteger total = new AtomicInteger();
    private volatile boolean closed;

    ConnectionPool(Factory factory, int maxSize) {
        this.factory = factory;
        this.maxSize = Math.max(1, maxSize);
        this.idle = new ArrayBlockingQueue<>(this.maxSize);
    }

    Connection borrow() throws SQLException {
        if (closed) {
            throw new SQLException("Connection-Pool ist geschlossen");
        }
        Connection c = idle.poll();
        if (c != null) {
            if (valid(c)) {
                return c;
            }
            quietClose(c);
            total.decrementAndGet();
        }

        // Neue Verbindung erzeugen, solange unter dem Limit.
        while (true) {
            int t = total.get();
            if (t >= maxSize) {
                break;
            }
            if (total.compareAndSet(t, t + 1)) {
                try {
                    return factory.open();
                } catch (SQLException e) {
                    total.decrementAndGet();
                    throw e;
                }
            }
        }

        // Limit erreicht: auf eine zurueckgegebene Verbindung warten.
        try {
            c = idle.poll(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Unterbrochen beim Warten auf eine Verbindung");
        }
        if (c == null) {
            throw new SQLException("Keine freie Verbindung verfuegbar (Pool erschoepft)");
        }
        if (!valid(c)) {
            quietClose(c);
            total.decrementAndGet();
            return borrow();
        }
        return c;
    }

    void release(Connection c) {
        if (c == null) {
            return;
        }
        if (closed || !idle.offer(c)) {
            quietClose(c);
            total.decrementAndGet();
        }
    }

    @Override
    public void close() {
        closed = true;
        Connection c;
        while ((c = idle.poll()) != null) {
            quietClose(c);
            total.decrementAndGet();
        }
    }

    private boolean valid(Connection c) {
        try {
            return !c.isClosed() && c.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    private static void quietClose(Connection c) {
        try {
            c.close();
        } catch (SQLException ignored) {
            // egal
        }
    }
}
