package it.markus.playerstats.config;

import it.markus.playerstats.unit.Units;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
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
    private final long minValue;

    private final boolean showShareButton;
    private final boolean abbreviateNumbers;
    private final boolean showMedals;
    private final int topPageSize;

    private final int indexRefreshIntervalSeconds;
    private final int indexWarmupPerTick;

    private final String storageType;
    private final int flushIntervalSeconds;
    private final String tablePrefix;
    private final String mysqlHost;
    private final int mysqlPort;
    private final String mysqlDatabase;
    private final String mysqlUser;
    private final String mysqlPassword;

    private final boolean trackElytraTime;
    private final boolean trackArrowAccuracy;
    private final boolean trackHeadshots;

    private final boolean updaterEnabled;
    private final boolean updaterNotifyOps;
    private final boolean updaterAutoDownload;
    private final String updaterSource;
    private final String updaterChannel;
    private final String updaterGithubRepo;
    private final String updaterVersionUrl;
    private final String updaterDownloadUrl;
    private final int updaterCheckIntervalHours;

    private final boolean discordEnabled;
    private final String discordWebhookUrl;
    private final String discordUsername;
    private final String discordAvatarUrl;
    private final boolean discordUseEmbeds;
    private final Map<String, Boolean> discordEvents;
    private final Map<String, String> discordMessages;

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
        this.minValue = Math.max(0L, c.getLong("filters.min-value", 0L));

        this.showShareButton = c.getBoolean("display.show-share-button", true);
        this.abbreviateNumbers = c.getBoolean("display.abbreviate-numbers", false);
        this.showMedals = c.getBoolean("display.show-medals", true);
        this.topPageSize = Math.max(1, c.getInt("display.top-page-size", 10));

        this.indexRefreshIntervalSeconds = Math.max(30, c.getInt("index.refresh-interval-seconds", 300));
        this.indexWarmupPerTick = Math.max(0, c.getInt("index.warmup-per-tick", 5));

        this.storageType = c.getString("storage.type", "yaml");
        this.flushIntervalSeconds = Math.max(5, c.getInt("storage.flush-interval-seconds", 30));
        this.tablePrefix = c.getString("storage.mysql.table-prefix", "ps_");
        this.mysqlHost = c.getString("storage.mysql.host", "localhost");
        this.mysqlPort = c.getInt("storage.mysql.port", 3306);
        this.mysqlDatabase = c.getString("storage.mysql.database", "playerstats");
        this.mysqlUser = c.getString("storage.mysql.user", "root");
        this.mysqlPassword = c.getString("storage.mysql.password", "");

        this.trackElytraTime = c.getBoolean("custom.track-elytra-time", true);
        this.trackArrowAccuracy = c.getBoolean("custom.track-arrow-accuracy", true);
        this.trackHeadshots = c.getBoolean("custom.track-headshots", false);

        this.updaterEnabled = c.getBoolean("updater.enabled", true);
        this.updaterNotifyOps = c.getBoolean("updater.notify-ops", true);
        this.updaterAutoDownload = c.getBoolean("updater.auto-download", false);
        this.updaterSource = c.getString("updater.source", "github");
        this.updaterChannel = c.getString("updater.channel", "stable");
        this.updaterGithubRepo = c.getString("updater.github-repo", "");
        this.updaterVersionUrl = c.getString("updater.version-url", "");
        this.updaterDownloadUrl = c.getString("updater.download-url", "");
        this.updaterCheckIntervalHours = Math.max(0, c.getInt("updater.check-interval-hours", 12));

        this.discordEnabled = c.getBoolean("discord.enabled", false);
        this.discordWebhookUrl = c.getString("discord.webhook-url", "");
        this.discordUsername = c.getString("discord.username", "PlayerStats");
        this.discordAvatarUrl = c.getString("discord.avatar-url", "");
        this.discordUseEmbeds = c.getBoolean("discord.use-embeds", true);
        this.discordEvents = boolMap(c.getConfigurationSection("discord.events"));
        this.discordMessages = stringMap(c.getConfigurationSection("discord.messages"));
    }

    private static Map<String, Boolean> boolMap(ConfigurationSection section) {
        Map<String, Boolean> map = new HashMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                map.put(key, section.getBoolean(key));
            }
        }
        return map;
    }

    private static Map<String, String> stringMap(ConfigurationSection section) {
        Map<String, String> map = new HashMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                map.put(key, section.getString(key, ""));
            }
        }
        return map;
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

    public long minValue() {
        return minValue;
    }

    public boolean showShareButton() {
        return showShareButton;
    }

    public boolean abbreviateNumbers() {
        return abbreviateNumbers;
    }

    public boolean showMedals() {
        return showMedals;
    }

    public int topPageSize() {
        return topPageSize;
    }

    public int indexRefreshIntervalSeconds() {
        return indexRefreshIntervalSeconds;
    }

    public int indexWarmupPerTick() {
        return indexWarmupPerTick;
    }

    public String storageType() {
        return storageType;
    }

    public int flushIntervalSeconds() {
        return flushIntervalSeconds;
    }

    public String tablePrefix() {
        return tablePrefix;
    }

    public String mysqlHost() {
        return mysqlHost;
    }

    public int mysqlPort() {
        return mysqlPort;
    }

    public String mysqlDatabase() {
        return mysqlDatabase;
    }

    public String mysqlUser() {
        return mysqlUser;
    }

    public String mysqlPassword() {
        return mysqlPassword;
    }

    public boolean trackElytraTime() {
        return trackElytraTime;
    }

    public boolean trackArrowAccuracy() {
        return trackArrowAccuracy;
    }

    public boolean trackHeadshots() {
        return trackHeadshots;
    }

    public boolean updaterEnabled() {
        return updaterEnabled;
    }

    public boolean updaterNotifyOps() {
        return updaterNotifyOps;
    }

    public boolean updaterAutoDownload() {
        return updaterAutoDownload;
    }

    public String updaterSource() {
        return updaterSource;
    }

    /** Release-Kanal: "stable" oder "prerelease". */
    public String updaterChannel() {
        return updaterChannel;
    }

    public String updaterGithubRepo() {
        return updaterGithubRepo;
    }

    public String updaterVersionUrl() {
        return updaterVersionUrl;
    }

    public String updaterDownloadUrl() {
        return updaterDownloadUrl;
    }

    public int updaterCheckIntervalHours() {
        return updaterCheckIntervalHours;
    }

    public boolean discordEnabled() {
        return discordEnabled;
    }

    public String discordWebhookUrl() {
        return discordWebhookUrl;
    }

    public String discordUsername() {
        return discordUsername;
    }

    public String discordAvatarUrl() {
        return discordAvatarUrl;
    }

    public boolean discordUseEmbeds() {
        return discordUseEmbeds;
    }

    /** Ist ein bestimmtes Discord-Ereignis aktiviert? (Default: aus) */
    public boolean discordEvent(String key) {
        return discordEvents.getOrDefault(key, false);
    }

    /** Nachrichtenvorlage fuer ein Discord-Ereignis (Fallback: der Schluessel). */
    public String discordMessage(String key) {
        return discordMessages.getOrDefault(key, key);
    }
}
