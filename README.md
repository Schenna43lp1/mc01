# PlayerStats

Konfigurierbares Spielerstatistik-Plugin für **Paper** (Minecraft 1.21+, Java 25).
40+ Statistiken, Top-Listen, Vergleiche, eigenes Event-Tracking, Lokalisierung,
YAML/SQLite/MySQL-Speicher, Discord-Webhook und Auto-Updater.

🌐 **Webseite/Doku:** https://schenna43lp1.github.io/mc01/ &nbsp;·&nbsp;
⬇ **Download:** [Releases](https://github.com/Schenna43lp1/mc01/releases)
![Java](https://img.shields.io/badge/Java-21-orange) ![Minecraft](https://img.shields.io/badge/Minecraft-1.21+-green) ![License](https://img.shields.io/github/license/schenna43lp1/mc01) ![Release](https://img.shields.io/github/v/release/schenna43lp1/mc01) ![Issues](https://img.shields.io/github/issues/schenna43lp1/mc01)
---

## Features

- **40+ Statistiken** in vier Kategorien: Vanilla (live), Gruppen (Summen), Custom (event-getrackt) und Berechnet (z. B. K/D, Trefferquote).
- **Top-Listen** mit Paginierung, Medaillen für die Plätze 1–3 und klickbarer Navigation; dazu Server-Gesamt, Server-Durchschnitt und Ranglisten-Platz.
- **Spieler-Vergleich** zweier Spieler inkl. Differenz und Führendem.
- **Eigenes Event-Tracking**: Kill-/Login-Streaks, Elytra-Flugzeit, Pfeil-Trefferquote, seltene Blöcke, geerntete Pflanzen, Honig, Beeren u. v. m.
- **Lokalisierung**: Vanilla-Statistiken werden clientseitig in die Sprache jedes Spielers übersetzt; eigene Namen über `language.yml`.
- **Smarte Einheiten**: Zeit, Distanz und Schaden automatisch oder fest formatiert (Ticks→Tage, cm→km, Rohwert→Herzen).
- **Volles Styling**: Farben als Minecraft-Namen oder Hex, plus Kursiv-, Festtags- und Regenbogen-Modus.
- **Speicher-Backends**: YAML, SQLite oder MySQL – frei wählbar, Custom-Werte werden gepuffert und asynchron gesammelt geschrieben.
- **Filter**: Whitelist, gebannte Spieler ausblenden, Mindestwerte, manuell ausgeschlossene Spieler.
- **Discord-Webhook**: Server-Status, Joins/Quits und Update-Hinweise als Embeds.
- **Auto-Updater**: Prüfung via GitHub-Releases, OP-Benachrichtigung beim Beitreten, optionaler Auto-Download.
- **Reload zur Laufzeit** – Konfiguration, Sprache und Gruppen ohne Server-Neustart.
- **Keine externen Abhängigkeiten** – JDBC-Treiber sind ins JAR gebündelt.

## Installation

1. Die neueste `player-stats.jar` aus den [Releases](https://github.com/Schenna43lp1/mc01/releases) laden.
2. In den Ordner `plugins/` deines Paper-Servers (1.21+) legen.
3. Server starten – `config.yml` und `language.yml` werden automatisch erzeugt.
4. Anpassen und mit `/playerstats reload` übernehmen.

## Befehle

Alles läuft über **einen** Befehl `/playerstats` (Aliase: `pstats`, `pstat`) – voll Tab-vervollständigt.
`/playerstats help` zeigt die ganze Liste im Spiel.

| Befehl | Beschreibung | Permission |
|--------|--------------|------------|
| `/playerstats <stat> [me\|top\|server\|player <name>]` | Eigene/fremde Werte, Top-Liste, Server-Gesamt | `playerstats.stat` |
| `/playerstats <stat> top [seite]` | Paginierte Rangliste | `playerstats.stat` |
| `/playerstats rank <stat> [spieler]` | Platz in der Rangliste | `playerstats.stat` |
| `/playerstats average <stat>` | Server-Durchschnitt | `playerstats.stat` |
| `/playerstats compare <stat> <sp1> [sp2]` | Zwei Spieler vergleichen | `playerstats.stat` |
| `/playerstats list` · `search <text>` · `info <stat>` | Katalog auflisten, suchen, Details | `playerstats.stat` |
| `/playerstats help` · `version` | Übersicht & Versions-/Update-Status | `playerstats.stat` |
| `/playerstats share` | Letztes Ergebnis im Chat teilen | `playerstats.share` |
| `/playerstats exclude <add\|remove\|list> [spieler]` | Spieler aus Statistiken ausschließen | `playerstats.exclude` |
| `/playerstats reset <spieler> [stat\|all]` | Custom-Statistiken zurücksetzen | `playerstats.reset` |
| `/playerstats reload` | Konfiguration neu laden | `playerstats.reload` |

### Permissions

| Permission | Standard | Zweck |
|------------|----------|-------|
| `playerstats.stat` | alle | Statistiken abfragen |
| `playerstats.share` | alle | Ergebnisse im Chat teilen |
| `playerstats.stats.others` | OP | fremde Werte/Ränge ansehen |
| `playerstats.exclude` | OP | Spieler ausschließen |
| `playerstats.reset` | OP | Custom-Statistiken zurücksetzen |
| `playerstats.reload` | OP | Konfiguration neu laden |

## Konfiguration

Alles in der `config.yml` (zur Laufzeit neu ladbar, Backend-Wechsel ausgenommen):

- **`units`** – Einheiten für Zeit/Distanz/Schaden (`auto`, `ticks`…`days`, `cm`/`blocks`/`km`, `raw`/`hp`/`hearts`).
- **`style`** – Farben (Name oder Hex), Kursiv, Festtags- und Regenbogen-Modus.
- **`display`** – Teilen-Button, Zahlen-Abkürzung, Medaillen, Seitengröße.
- **`filters`** – Whitelist, gebannte ausschließen, Mindestwert, Listengröße, Letzter-Login-Fenster.
- **`storage`** – `yaml` | `sqlite` | `mysql` mit Flush-Intervall (MySQL-Zugang separat).
- **`custom`** – Elytra-Zeit, Trefferquote, Headshots, seltene Blöcke.
- **`updater`** – Update-Prüfung via GitHub-Releases, mit wählbarem Kanal (`channel: stable` | `prerelease`) und optionalem Auto-Download.
- **`discord`** – Webhook-URL und schaltbare Ereignisse. **Webhook-URL ist ein Geheimnis – nie committen!**
- **`groups`** – frei erweiterbare Material-/Statistik-Gruppen (Ores, Logs, Crops, Distanz).

## Statistik-Kategorien

- **Vanilla** – live aus `OfflinePlayer.getStatistic(...)`, read-only (Spielzeit, Tode, Kills, Distanzen, …).
- **Gruppen** – Summen mehrerer Vanilla-Werte (Gesamtdistanz, abgebaute Blöcke, Holz, Erze, Diamanten …).
- **Custom** – event-getrackt und im Storage gespeichert (Streaks, Trefferquote-Rohdaten, seltene Blöcke …).
- **Berechnet** – zur Laufzeit abgeleitet, nie gespeichert (K/D, Trefferquote).

## Aus dem Quellcode bauen

Benötigt **JDK 25** (Paper 26.x ist für Java 25 kompiliert).

```bash
cd playerstats
mvn clean package
# -> target/player-stats.jar (geshadet, inkl. JDBC-Treiber)
```

## Hinweise

- Dieses Repository enthält außerdem zwei kleinere Lern-Plugins: **FirstPlugin** (Projekt-Root) und **ServerStats** (`stats-plugin/`). Das Release-/Webseiten-Produkt ist **PlayerStats** (`playerstats/`).
- Gebaut für Paper 1.21+ · Java 25.

---
🐞 Reporting Bugs

Found a bug?

Please create an Issue with:

Minecraft version
Paper/Spigot version
Plugin version
Error log
Steps to reproduce
💡 Feature Requests

Feature ideas are always welcome.

Open an Issue and describe your suggestion.

🤝 Contributing

Pull Requests are welcome!

Fork the repository
Create a feature branch
Commit your changes
Open a Pull Request
📄 License

This project is licensed under the MIT License.

⭐ Support

If you like this project:

⭐ Star the repository

🍴 Fork it

🐛 Report bugs

💬 Suggest new features
© PlayerStats · von Markus
