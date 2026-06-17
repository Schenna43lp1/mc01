package it.markus.playerstats.stat;

/**
 * Welche Art von Wert eine Statistik liefert – bestimmt die Einheiten-Umrechnung
 * und Formatierung.
 */
public enum StatCategory {
    /** Reine Anzahl (Tode, Kills, ...). */
    GENERIC,
    /** Zeit in Ticks (20 Ticks = 1 Sekunde). */
    TIME,
    /** Strecke in Zentimetern. */
    DISTANCE,
    /** Schaden im Rohwert (1/10 Lebenspunkt). */
    DAMAGE,
    /** Zeitpunkt als Epoch-Millis (z. B. erstes Join-Datum). */
    DATE,
    /** Verhaeltniszahl, z. B. K/D (zwei Nachkommastellen). */
    RATIO,
    /** Prozentwert, z. B. Trefferquote (Wert ist bereits 0..100). */
    PERCENT
}
