package it.markus.playerstats.config;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * Parst Farb-Strings aus der Config: entweder ein Minecraft-Farbname
 * (z. B. "gold", "dark_red") ODER ein Hex-Wert (z. B. "#FF8800").
 */
public final class ColorParser {

    private ColorParser() {
    }

    public static TextColor parse(String raw, TextColor fallback, Logger log) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String value = raw.trim();

        if (value.startsWith("#")) {
            TextColor hex = TextColor.fromHexString(value);
            if (hex != null) {
                return hex;
            }
        } else {
            NamedTextColor named = NamedTextColor.NAMES.value(value.toLowerCase(Locale.ROOT));
            if (named != null) {
                return named;
            }
        }

        if (log != null) {
            log.warning("Unbekannte Farbe '" + raw + "' – nutze Standardfarbe.");
        }
        return fallback;
    }
}
