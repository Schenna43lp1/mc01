package it.markus.playerstats.format;

import it.markus.playerstats.config.PluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Baut aus Roh-Text/Komponenten die fertig gestylten Chat-Komponenten:
 * Farben (Standard oder Hex), optionales Kursiv, festliche Palette und
 * Regenbogen-Verlauf.
 */
public final class StyleFormatter {

    // Festliche Palette (rot/gruen/gold) – wird zyklisch pro Zeichen genutzt.
    private static final TextColor[] FESTIVE = {
            TextColor.color(0xE3_3B_3B),
            TextColor.color(0x2E_A0_44),
            TextColor.color(0xF2_C0_4D)
    };

    private final PluginConfig cfg;

    public StyleFormatter(PluginConfig cfg) {
        this.cfg = cfg;
    }

    public Component text(String s) {
        return italic(Component.text(s, cfg.textColor()));
    }

    public Component value(String s) {
        return italic(Component.text(s, cfg.numberColor()));
    }

    public Component playerName(String s) {
        return italic(Component.text(s, cfg.playerNameColor()));
    }

    public Component rank(int rank) {
        return italic(Component.text("#" + rank, cfg.rankColor()));
    }

    /** Medaille fuer die Plaetze 1–3, sonst normaler Rang. */
    public Component medal(int rank) {
        String emoji = switch (rank) {
            case 1 -> "🥇"; // 🥇
            case 2 -> "🥈"; // 🥈
            case 3 -> "🥉"; // 🥉
            default -> null;
        };
        return emoji == null ? rank(rank) : italic(Component.text(emoji));
    }

    /** Faerbt einen (ggf. uebersetzbaren) Statistik-Namen ein. */
    public Component statName(Component rawName) {
        Component c = rawName.color(cfg.statNameColor());
        return cfg.italic() ? c.decoration(TextDecoration.ITALIC, true) : c;
    }

    /** Titelzeile – Regenbogen schlaegt Festive schlaegt einfache Farbe. */
    public Component title(String s) {
        if (cfg.rainbow()) {
            return rainbow(s);
        }
        if (cfg.festive()) {
            return festive(s);
        }
        return italic(Component.text(s, cfg.titleColor()));
    }

    // --- intern ------------------------------------------------------------

    private Component italic(Component c) {
        return cfg.italic() ? c.decoration(TextDecoration.ITALIC, true) : c;
    }

    private Component rainbow(String s) {
        TextComponent.Builder b = Component.text();
        int len = s.length();
        for (int i = 0; i < len; i++) {
            float hue = len <= 1 ? 0f : (float) i / len;
            b.append(Component.text(s.charAt(i), hsv(hue)));
        }
        return italic(b.build());
    }

    private Component festive(String s) {
        TextComponent.Builder b = Component.text();
        for (int i = 0; i < s.length(); i++) {
            b.append(Component.text(s.charAt(i), FESTIVE[i % FESTIVE.length]));
        }
        return italic(b.build());
    }

    /** HSV (volle Saettigung/Helligkeit) -> TextColor. */
    private static TextColor hsv(float h) {
        float s = 1f, v = 1f;
        int i = (int) (h * 6) % 6;
        float f = h * 6 - (int) (h * 6);
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        float r, g, bl;
        switch (i) {
            case 0 -> { r = v; g = t; bl = p; }
            case 1 -> { r = q; g = v; bl = p; }
            case 2 -> { r = p; g = v; bl = t; }
            case 3 -> { r = p; g = q; bl = v; }
            case 4 -> { r = t; g = p; bl = v; }
            default -> { r = v; g = p; bl = q; }
        }
        return TextColor.color(Math.round(r * 255), Math.round(g * 255), Math.round(bl * 255));
    }
}
