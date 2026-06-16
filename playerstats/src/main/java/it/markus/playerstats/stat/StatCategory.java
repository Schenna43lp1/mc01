package it.markus.playerstats.stat;

/**
 * Welche Art von Wert eine Statistik liefert – bestimmt die Einheiten-Umrechnung.
 */
public enum StatCategory {
    /** Reine Anzahl (Tode, Kills, ...). */
    GENERIC,
    /** Zeit in Ticks (20 Ticks = 1 Sekunde). */
    TIME,
    /** Strecke in Zentimetern. */
    DISTANCE,
    /** Schaden im Rohwert (1/10 Lebenspunkt). */
    DAMAGE
}
