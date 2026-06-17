package it.markus.playerstats.stat;

/**
 * Beschreibt eine einzelne Statistik der Registry.
 *
 * @param id             stabile id fuer Command/Config/language.yml
 * @param category       fuer Einheiten-Umrechnung/Formatierung
 * @param translationKey Minecraft-Key fuer clientseitige Uebersetzung
 *                       (null bei CUSTOM/COMPUTED/GROUP -> Name kommt aus language.yml)
 * @param kind           Herkunft des Werts
 * @param resolver       berechnet den Wert je Spieler
 * @param customKey      Storage-Schluessel bei CUSTOM-Stats (sonst null) –
 *                       wird fuer /statreset und /playerstats info gebraucht
 */
public record StatDefinition(String id,
                             StatCategory category,
                             String translationKey,
                             StatKind kind,
                             StatResolver resolver,
                             String customKey) {
}
