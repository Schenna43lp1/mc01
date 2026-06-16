package it.markus.playerstats.config;

import it.markus.playerstats.unit.Units;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;

/**
 * Unveraenderlicher Schnappschuss der config.yml mit getypten Gettern.
 *
 * Bei /playerstats reload wird einfach eine neue Instanz gebaut – so kann es
 * keine halb-aktualisierten Zustaende geben.
 */
public final class PluginConfig {

    private final Units.Time timeUnit;
    private final Units.Distance distanceUnit;
    private final Units.Damage damageUnit;
    private final int autoDetail;

    private final TextColor textColor;
    private final TextColor numberColor;
    private final TextColor statNameColor;
    private final TextColor playerNameColor;
    private final TextColor titleColor;
    private final TextColor rankColor;
    private final boolean italic;
    private final boolean festive;
    private final boolean rainbow;

    private final boolean useWhitelist;
    private final boolean excludeBanned;
    private final int maxLastJoinDays;
    private final int topListSize;

    public PluginConfig(FileConfiguration c, Logger log) {
        this.timeUnit = Units.time(c.getString("units.time"), Units.Time.AUTO);
        this.distanceUnit = Units.distance(c.getString("units.distance"), Units.Distance.BLOCKS);
        this.damageUnit = Units.damage(c.getString("units.damage"), Units.Damage.HEARTS);
        this.autoDetail = Math.max(1, c.getInt("units.auto-detail", 2));

        this.textColor = ColorParser.parse(c.getString("style.text-color"), NamedTextColor.GRAY, log);
        this.numberColor = ColorParser.parse(c.getString("style.number-color"), NamedTextColor.GREEN, log);
        this.statNameColor = ColorParser.parse(c.getString("style.stat-name-color"), NamedTextColor.GOLD, log);
        this.playerNameColor = ColorParser.parse(c.getString("style.player-name-color"), NamedTextColor.YELLOW, log);
        this.titleColor = ColorParser.parse(c.getString("style.title-color"), NamedTextColor.GOLD, log);
        this.rankColor = ColorParser.parse(c.getString("style.rank-color"), NamedTextColor.YELLOW, log);
        this.italic = c.getBoolean("style.italic", false);
        this.festive = c.getBoolean("style.festive", false);
        this.rainbow = c.getBoolean("style.rainbow", false);

        this.useWhitelist = c.getBoolean("filters.use-whitelist", false);
        this.excludeBanned = c.getBoolean("filters.exclude-banned", true);
        this.maxLastJoinDays = Math.max(0, c.getInt("filters.max-last-join-days", 0));
        this.topListSize = Math.max(1, c.getInt("filters.top-list-size", 10));
    }

    public Units.Time timeUnit() {
        return timeUnit;
    }

    public Units.Distance distanceUnit() {
        return distanceUnit;
    }

    public Units.Damage damageUnit() {
        return damageUnit;
    }

    public int autoDetail() {
        return autoDetail;
    }

    public TextColor textColor() {
        return textColor;
    }

    public TextColor numberColor() {
        return numberColor;
    }

    public TextColor statNameColor() {
        return statNameColor;
    }

    public TextColor playerNameColor() {
        return playerNameColor;
    }

    public TextColor titleColor() {
        return titleColor;
    }

    public TextColor rankColor() {
        return rankColor;
    }

    public boolean italic() {
        return italic;
    }

    public boolean festive() {
        return festive;
    }

    public boolean rainbow() {
        return rainbow;
    }

    public boolean useWhitelist() {
        return useWhitelist;
    }

    public boolean excludeBanned() {
        return excludeBanned;
    }

    public int maxLastJoinDays() {
        return maxLastJoinDays;
    }

    public int topListSize() {
        return topListSize;
    }
}
