package it.markus.serverstats;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Kleiner Helfer, um Konfig-Strings einheitlich als MiniMessage zu rendern.
 *
 * Platzhalter werden als Paare uebergeben:
 *   Msg.render(text, "%player%", name, "%online%", "5")
 */
public final class Msg {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private Msg() {
        // Utility-Klasse – nicht instanziieren.
    }

    public static Component render(String raw, String... placeholderPairs) {
        String text = raw == null ? "" : raw;
        for (int i = 0; i + 1 < placeholderPairs.length; i += 2) {
            text = text.replace(placeholderPairs[i], placeholderPairs[i + 1]);
        }
        return MINI.deserialize(text);
    }
}
