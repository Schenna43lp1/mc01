package it.markus.playerstats.stat;

/**
 * Herkunft eines Statistik-Werts – die zentrale Trennung des Spec:
 *
 *   VANILLA       – live aus {@code OfflinePlayer.getStatistic(...)}, read-only.
 *   VANILLA_GROUP – Summe mehrerer Vanilla-Statistiken (z. B. alle Distanzen,
 *                   alle Log-Bloecke). Material-/Stat-Listen kommen aus der Config.
 *   CUSTOM        – eigene, event-getrackte Daten aus dem Storage.
 *   COMPUTED      – zur Laufzeit aus anderen Werten abgeleitet (K/D, Quote);
 *                   wird NIE gespeichert.
 */
public enum StatKind {
    VANILLA,
    VANILLA_GROUP,
    CUSTOM,
    COMPUTED
}
