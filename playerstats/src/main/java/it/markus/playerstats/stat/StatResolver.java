package it.markus.playerstats.stat;

/**
 * Berechnet den Wert einer Statistik fuer einen Spieler.
 *
 * Wird (fuer Top-/Server-Abfragen) im Async-Thread aufgerufen – Implementierungen
 * duerfen nur threadsichere Quellen nutzen: {@code OfflinePlayer.getStatistic}
 * (Disk-Read) und den threadsicheren {@code CustomStatService}-Cache.
 */
@FunctionalInterface
public interface StatResolver {
    double resolve(StatContext ctx);
}
