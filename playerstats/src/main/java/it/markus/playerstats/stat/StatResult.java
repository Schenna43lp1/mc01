package it.markus.playerstats.stat;

/**
 * Ein Statistik-Ergebnis fuer einen Spieler (Name + Wert).
 *
 * Der Wert ist {@code double}, damit auch berechnete Statistiken (K/D,
 * Trefferquote) ohne Sonderbehandlung sortier- und darstellbar sind.
 */
public record StatResult(String playerName, double value) {
}
