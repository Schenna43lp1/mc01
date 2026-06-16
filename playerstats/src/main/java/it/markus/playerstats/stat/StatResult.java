package it.markus.playerstats.stat;

/**
 * Ein Statistik-Ergebnis fuer einen Spieler (Name + Rohwert).
 */
public record StatResult(String playerName, long value) {
}
