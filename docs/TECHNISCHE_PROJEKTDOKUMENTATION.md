# Technische Projektdokumentation - PlayerStats

## 1. Projektueberblick

**PlayerStats** ist ein Paper-Plugin fuer Minecraft 1.21+, das Spielerstatistiken sammelt, darstellt und auswertet.  
Hauptmodul und Release-Produkt ist `playerstats/`.

Ziele:

- konsistente Statistikabfragen ueber einen zentralen Befehl (`/playerstats`)
- performante Ranglisten (Index statt Vollscan bei jeder Abfrage)
- flexible Speicherung von Custom-Stats (YAML, SQLite, MySQL)
- konfigurierbare Darstellung, Filter, Lokalisierung und Integrationen

## 2. Technischer Stack

| Bereich | Technologie |
|---|---|
| Sprache | Java 25 |
| Build | Maven |
| Minecraft-API | Paper API `26.1.2.build.70-stable` |
| Speicher-Backends | YAML, SQLite, MySQL/MariaDB |
| Packaging | Maven Shade (inkl. JDBC-Treiber) |

## 3. Repository-Struktur

| Pfad | Zweck |
|---|---|
| `playerstats/` | Hauptplugin (produktiver Code) |
| `src/` (Root) | Lern-/Beispielplugin `first-plugin` |
| `stats-plugin/` | zusaetzliches Lernplugin `ServerStats` |
| `docs/` | Projektseite und Dokumentation |

## 4. Laufzeitarchitektur

Einstiegspunkt: `it.markus.playerstats.PlayerStatsPlugin`

Kernkomponenten:

- **Konfiguration/UI:** `PluginConfig`, `StyleFormatter`, `LanguageManager`
- **Statistiken:** `StatRegistry`, `StatService`, `StatIndex`
- **Custom-Tracking:** `CustomStatService` + Event-Listener
- **Storage-Abstraktion:** `StorageProvider`, `StorageFactory`
- **Moderation/Filter:** `ExcludeManager`, `PlayerFilter`
- **Integrationen:** `DiscordNotifier`, `UpdateChecker`
- **Commands:** `StatCommand`, `StatCompareCommand`, `StatExcludeCommand`, `StatResetCommand`

## 5. Datenfluss

1. Events (z. B. Combat, Mining, Session) erhoehen Custom-Counter im Speicher.
2. `CustomStatService` puffert Aenderungen und schreibt sie gesammelt per Flush-Intervall.
3. Abfragen (`/playerstats ...`) kombinieren Vanilla-, Gruppen-, Custom- und berechnete Werte.
4. Top/Rank/Average nutzen den `StatIndex` (In-Memory), der regelmaessig aufgefrischt wird.

## 6. Speicher-Backends und Persistenz

| Backend | Speicherort/Schema | Besonderheiten |
|---|---|---|
| YAML | `plugins/PlayerStats/custom-stats.yml` | einfaches Flat-File, kein externer Dienst |
| SQLite | `plugins/PlayerStats/playerstats.db` | WAL-Modus, lokales DB-File, Pool-Groesse 1 |
| MySQL | Tabelle `<prefix>custom_stats` | externer DB-Server, konfigurierbarer Connection-Pool |

JDBC-Schema:

- Primarschluessel: `(uuid, stat_key)`
- Spalten: `uuid`, `stat_key`, `value`
- Upsert pro Backend (SQLite `ON CONFLICT`, MySQL `ON DUPLICATE KEY`)

## 7. Lebenszyklus

### Start (`onEnable`)
1. Config laden (`saveDefaultConfig` + `PluginConfig`).
2. Komponenten initialisieren.
3. Storage initialisieren, bei Fehlern Fallback auf YAML.
4. Command + Listener registrieren.
5. Scheduler starten: Flush, Elytra-Tracking, Update-Check, Index-Refresh.

### Reload (`/playerstats reload`)

- laedt `config.yml`, `language.yml` und Gruppen neu
- **kein** Live-Wechsel des Storage-Typs (dafuer Neustart notwendig)

### Stop (`onDisable`)

- Discord-Offline-Event senden
- `CustomStatService` sauber herunterfahren

## 8. Wichtige Konfigurationsbereiche (`config.yml`)

| Bereich | Zweck |
|---|---|
| `units` | Formatierung fuer Zeit, Distanz, Schaden |
| `style` / `display` | Farben, Ausgabeverhalten, Top-Listen-Seitengroesse |
| `filters` | Whitelist/Ban-Filter, Mindestwerte, Top-Limits |
| `index` | Refresh-Intervall und Warmup fuer Leaderboard-Index |
| `storage` | Backend, Flush-Intervall, MySQL-Zugangsdaten |
| `custom` | Aktivierung einzelner Event-Tracker |
| `updater` | GitHub/URL-Quelle, Intervall, optionaler Auto-Download |
| `discord` | Webhook, Eventauswahl, Nachrichtenvorlagen |
| `groups` | frei definierbare Statistik-/Materialgruppen |

## 9. Command- und Permission-Modell

Hauptcommand: `/playerstats` (Aliase `pstats`, `pstat`)

Relevante Permissions:

- `playerstats.stat` (Grundabfragen)
- `playerstats.share`
- `playerstats.stats.others`
- `playerstats.exclude`
- `playerstats.reset`
- `playerstats.reload`

## 10. Build, Artefakte und Deployment

```bash
cd playerstats
mvn clean package
```

Ergebnis: `playerstats/target/player-stats.jar`  
Das Artefakt enthaelt die JDBC-Treiber (SQLite + MySQL) bereits im JAR.

Deployment:

1. JAR nach `plugins/` kopieren.
2. Server starten/restarten.
3. `config.yml` und `language.yml` anpassen.
4. Aenderungen mit `/playerstats reload` aktivieren (sofern kein Backend-Wechsel).

## 11. Betrieb und Wartung

- Zielplattform: **Paper 1.21+**, Runtime: **Java 25**
- Secrets (z. B. `discord.webhook-url`, DB-Passwoerter) nicht committen
- bei MySQL regelmaessig Erreichbarkeit/Latenz pruefen
- bei YAML/SQLite vor groesseren Updates Backup der Datenfiles anlegen
- Update-Kanal bewusst waehlen (`stable` vs. `prerelease`)

## 12. Bekannte Grenzen

- Wechsel von `storage.type` erfordert Neustart
- Headshot-Tracking ist optional und basiert auf Approximation
- Discord-Webhooks haben Rate-Limits; Join/Quit-Events ggf. drosseln/deaktivieren
