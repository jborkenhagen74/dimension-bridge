# DimensionBridge – Installations-, Konfigurations- und Befehlsanleitung

## 1. Überblick

DimensionBridge ist eine rein serverseitige Bridge zwischen Fabric-Backends und einem Velocity-Proxy. Sie erlaubt Commandblöcken, Konsole und entsprechend privilegierten Befehlsquellen, Spieler auf einen registrierten Velocity-Server zu übertragen.

Der Spieler benötigt dafür weder den Velocity-Befehl `/server` noch eine eigene Bridge-Berechtigung. Geschützte Zielserver können so ausschließlich über freigegebene Portale, Telefonzellen oder andere Commandblock-Konstruktionen betreten werden.

### Architektur

```text
Minecraft-Client
       │
       ▼
Velocity-Proxy
└─ DimensionBridge Velocity Plugin
       │  Kanal: dimensionbridge:transfer
       ▼
Fabric-Backend
└─ DimensionBridge Fabric Mod
   └─ Commandblock: dimensionbridge transfer <Zielauswahl> <Server>
```

DimensionBridge benötigt keinen Client-Mod.

---

## 2. Unterstützte Versionen

Für jede native Fabric-Serverversion wird ein eigenes JAR gebaut:

```text
1.20.1  1.20.2  1.20.3  1.20.4  1.20.5  1.20.6
1.21    1.21.1  1.21.2  1.21.3  1.21.4  1.21.5
1.21.6  1.21.7  1.21.8  1.21.9  1.21.10 1.21.11
26.1    26.1.1  26.1.2  26.2
```

Verwende immer das Fabric-JAR, das exakt zur **nativen Version des jeweiligen Backend-Servers** passt. Die Clientversion, mit der sich ein Spieler über ViaVersion verbindet, ist hierfür nicht maßgeblich.

### Laufzeit-Java

| Minecraft-Version | Java |
|---|---:|
| 1.20.1–1.20.4 | 17 |
| 1.20.5–1.21.11 | 21 |
| 26.1–26.2 | 25 |
| aktuelles Velocity-Plugin | 25 |

---

## 3. Voraussetzungen

### Velocity

- Velocity-Proxy
- Java 25
- alle Backend-Server in `velocity.toml` registriert
- optional LuckPerms für die Berechtigungsverwaltung

### Fabric-Backends

- Fabric Loader passend zur Minecraft-Version
- Fabric API passend zur Minecraft-Version
- das passende DimensionBridge-Fabric-JAR
- Commandblöcke in `server.properties` aktiviert:

```properties
enable-command-block=true
```

Nach einer Änderung an `server.properties` ist ein vollständiger Serverneustart erforderlich.

---

## 4. Download aus GitHub Actions

Nach einem erfolgreichen Build:

1. GitHub-Repository öffnen.
2. **Actions** auswählen.
3. Den erfolgreichen Build-Workflow öffnen.
4. Unten den Bereich **Artifacts** öffnen.
5. Das Artefakt `dimensionbridge-all` herunterladen.
6. ZIP-Datei entpacken.

Verwende nur die normalen Release-JARs. Dateien mit Zusätzen wie `-sources.jar` oder `-dev.jar` gehören nicht auf den Server.

---

## 5. Installation auf Velocity

1. Velocity vollständig stoppen.
2. Das Velocity-JAR nach folgendem Ordner kopieren:

```text
velocity/plugins/dimensionbridge-velocity-<VERSION>.jar
```

3. Velocity starten.
4. Nach dem ersten Start wird folgende Datei erzeugt:

```text
plugins/dimensionbridge/dimensionbridge.properties
```

5. Velocity stoppen oder die Datei bearbeiten und anschließend mit `/dimensionbridge reload` neu laden.

Beim Start sollte in der Proxy-Konsole sinngemäß erscheinen:

```text
DimensionBridge wurde geladen. Kanal: dimensionbridge:transfer
```

---

## 6. Installation auf einem Fabric-Backend

Für jeden Backend-Server separat:

1. Backend vollständig stoppen.
2. Das exakt passende Fabric-JAR in den `mods`-Ordner kopieren.
3. Sicherstellen, dass Fabric API installiert ist.
4. Nur **ein** DimensionBridge-JAR im `mods`-Ordner belassen.
5. Backend starten.
6. Nach dem ersten Start wird erzeugt:

```text
config/dimensionbridge-fabric.properties
```

7. Datei bearbeiten und Backend neu starten.

Beim Start sollte sinngemäß erscheinen:

```text
DimensionBridge-Backend <VERSION> geladen. Kanal: dimensionbridge:transfer
```

Die Fabric-Konfiguration besitzt aktuell keinen Reload-Befehl. Änderungen an `dimensionbridge-fabric.properties` werden nach einem Serverneustart übernommen.

---

## 7. Fabric-Konfiguration

Datei:

```text
config/dimensionbridge-fabric.properties
```

Beispiel für die Lobby:

```properties
allowed-destinations=hauptwelt,hardcore,vanilla
```

Beispiel für einen Spielserver, der nur zurück zur Lobby verbinden darf:

```properties
allowed-destinations=lobby
```

Alle Ziele erlauben:

```properties
allowed-destinations=*
```

### `allowed-destinations`

Lokale Positivliste des jeweiligen Fabric-Backends. Ein Transfer wird nur als Plugin-Nachricht an Velocity gesendet, wenn das Ziel hier freigegeben ist.

- mehrere Namen werden durch Kommata getrennt;
- Groß-/Kleinschreibung wird normalisiert;
- Servernamen dürfen nur Kleinbuchstaben, Ziffern, `_` und `-` enthalten;
- `*` erlaubt jedes syntaktisch gültige Ziel, das Velocity anschließend akzeptiert.

Aus Sicherheitsgründen empfiehlt sich eine möglichst enge Liste statt `*`.

---

## 8. Velocity-Konfiguration

Datei:

```text
plugins/dimensionbridge/dimensionbridge.properties
```

Vollständiges Beispiel mit den Servern `lobby`, `hauptwelt`, `hardcore` und `vanilla`:

```properties
# Servernamen müssen exakt den Namen in velocity.toml entsprechen.
# * erlaubt alle Quellen beziehungsweise alle Protokolle.

initial-servers=lobby
deny-unlisted-targets=true
cooldown-ms=2500
authorization-window-ms=5000
bypass-permission=dimensionbridge.bypass

destinations=lobby,hauptwelt,hardcore,vanilla

destination.lobby.display-name=Lobby
destination.lobby.protected=false
destination.lobby.allowed-sources=*
destination.lobby.allowed-protocols=*

destination.hauptwelt.display-name=Hauptwelt
destination.hauptwelt.protected=true
destination.hauptwelt.allowed-sources=lobby
destination.hauptwelt.allowed-protocols=MINECRAFT_1_20

destination.hardcore.display-name=Hardcore
destination.hardcore.protected=true
destination.hardcore.allowed-sources=lobby
destination.hardcore.allowed-protocols=MINECRAFT_1_20

destination.vanilla.display-name=Vanilla
destination.vanilla.protected=true
destination.vanilla.allowed-sources=lobby
destination.vanilla.allowed-protocols=MINECRAFT_26_2
```

### Globale Einstellungen

#### `initial-servers`

Kommaseparierte Liste der Server, die beim ersten Proxy-Login als Einstiegsserver erlaubt sind.

```properties
initial-servers=lobby
```

#### `deny-unlisted-targets`

```properties
deny-unlisted-targets=true
```

- `true`: Server, die nicht unter `destinations` aufgeführt sind, werden abgewiesen.
- `false`: Nicht aufgeführte Server bleiben außerhalb der DimensionBridge-Regeln erreichbar.

Für ein geschlossenes Portalnetz ist `true` empfehlenswert.

#### `cooldown-ms`

Wartezeit zwischen zwei Bridge-Anfragen desselben Spielers.

```properties
cooldown-ms=2500
```

Erlaubter Bereich: 0 bis 60.000 Millisekunden.

#### `authorization-window-ms`

Zeitfenster, in dem der von DimensionBridge gestartete Velocity-Verbindungsversuch als autorisiert gilt.

```properties
authorization-window-ms=5000
```

Erlaubter Bereich: 500 bis 60.000 Millisekunden. Dieser Wert sollte größer als die normale Verbindungsdauer zwischen Proxy und Backend sein.

#### `bypass-permission`

Berechtigung, die alle DimensionBridge-Schutzprüfungen umgeht:

```properties
bypass-permission=dimensionbridge.bypass
```

Diese Berechtigung sollte nur Administratoren erhalten.

### Zieldefinitionen

Jeder Name in `destinations` benötigt einen Block aus vier Einstellungen:

```properties
destinations=lobby,hauptwelt

destination.hauptwelt.display-name=Hauptwelt
destination.hauptwelt.protected=true
destination.hauptwelt.allowed-sources=lobby
destination.hauptwelt.allowed-protocols=MINECRAFT_1_20
```

#### `display-name`

Anzeigename in Meldungen an Spieler.

#### `protected`

- `true`: Das Ziel benötigt eine gültige DimensionBridge-Autorisierung. Ein direkter `/server`-Wechsel wird blockiert, sofern der Spieler keine Bypass-Berechtigung besitzt.
- `false`: Das Ziel darf regulär betreten werden, solange die Protokollregel passt.

Die Lobby sollte normalerweise `protected=false` verwenden, damit Login und Rückkehr zuverlässig möglich sind.

#### `allowed-sources`

Liste der Backend-Server, von denen dieses Ziel angewählt werden darf.

```properties
destination.hauptwelt.allowed-sources=lobby
```

Mehrere Quellen:

```properties
destination.hauptwelt.allowed-sources=lobby,hardcore
```

Alle Quellen:

```properties
destination.lobby.allowed-sources=*
```

#### `allowed-protocols`

Liste der von Velocity erkannten Client-Protokolle.

```properties
destination.hauptwelt.allowed-protocols=MINECRAFT_1_20
```

Mehrere Protokolle:

```properties
destination.hauptwelt.allowed-protocols=MINECRAFT_1_20,MINECRAFT_1_21
```

Alle Protokolle:

```properties
destination.lobby.allowed-protocols=*
```

Minecraft 1.20 und 1.20.1 verwenden dieselbe Protokollgruppe `MINECRAFT_1_20`.

Den exakten Velocity-Namen eines verbundenen Clients ermittelst du mit:

```text
/dimensionbridge protocol <Spielername>
```

---

## 9. Berechtigungen

### `dimensionbridge.admin`

Erlaubt Spielern die administrativen Velocity-Befehle von DimensionBridge.

### `dimensionbridge.bypass`

Umgeht die Ziel-, Quellen-, Protokoll- und Portalautorisierungsprüfung. Nur vertrauenswürdigen Administratoren geben.

### `velocity.command.server`

Der normale Velocity-Befehl `/server`. Für ein ausschließlich portalbasiertes System kann er normalen Spielern entzogen werden:

```text
/lp group default permission set velocity.command.server false
```

Administratoren können ihn weiterhin erhalten:

```text
/lp group admin permission set velocity.command.server true
```

DimensionBridge benötigt `velocity.command.server` nicht.

### Backend-Befehl

`dimensionbridge transfer` ist auf Fabric nur für Konsole, Commandblöcke und Quellen mit Moderator-/Befehlsstufe 2 verfügbar. Normale Spieler können ihn nicht ausführen. Gib normalen Spielern daher keine OP- oder Moderatorrechte.

---

## 10. Befehlsübersicht

### Fabric-Backend

#### Spieler übertragen

```text
/dimensionbridge transfer <Zielauswahl> <Velocity-Servername>
```

Im Commandblock wird der führende Schrägstrich weggelassen:

```mcfunction
dimensionbridge transfer @p hauptwelt
```

Beispiele:

```mcfunction
# Nächster Spieler
dimensionbridge transfer @p hauptwelt

# Nächster Spieler innerhalb von drei Blöcken
dimensionbridge transfer @p[distance=..3,limit=1,sort=nearest] hauptwelt

# Alle Spieler in der Telefonzelle
dimensionbridge transfer @a[x=10,y=64,z=5,dx=0,dy=2,dz=0] hauptwelt

# Nur Spieler, die nicht im Kampf markiert sind
dimensionbridge transfer @a[x=10,y=64,z=5,distance=..2,tag=!inCombat] hauptwelt

# Rückkehr zur Lobby
dimensionbridge transfer @p[distance=..3,limit=1,sort=nearest] lobby
```

### Velocity

#### Hilfe

```text
/dimensionbridge
/db
```

#### Konfiguration neu laden

```text
/dimensionbridge reload
/db reload
```

Dies lädt nur die Velocity-Konfiguration neu.

#### Aktive Konfiguration anzeigen

```text
/dimensionbridge info
/db info
```

Die Ausgabe zeigt den Pfad der Konfigurationsdatei und die geladenen Ziele.

#### Client-Protokoll anzeigen

```text
/dimensionbridge protocol
/dimensionbridge protocol <Spielername>
```

Ohne Spielernamen wird bei Ausführung durch einen Spieler dessen eigenes Protokoll angezeigt.

---

## 11. Einfacher Funktionstest

### Commandblock

Einstellungen:

```text
Impuls
Unbedingt
Benötigt Redstone
```

Befehl:

```mcfunction
dimensionbridge transfer @p[distance=..3,limit=1,sort=nearest] hauptwelt
```

Stelle dich höchstens drei Blöcke entfernt auf und drücke den Knopf.

Zum Ausschluss eines Entfernungsfehlers:

```mcfunction
dimensionbridge transfer @p hauptwelt
```

In der vorherigen Ausgabe des Commandblocks sollte sinngemäß stehen:

```text
DimensionBridge: 1 Transferanfrage(n) nach 'hauptwelt' gesendet.
```

---

## 12. Beispiel: Lobby-Telefonzellen

### Telefonzelle zur Hauptwelt

```mcfunction
dimensionbridge transfer @p[distance=..3,limit=1,sort=nearest] hauptwelt
```

### Telefonzelle zur Hardcore-Welt

```mcfunction
dimensionbridge transfer @p[distance=..3,limit=1,sort=nearest] hardcore
```

### Telefonzelle zur Vanilla-Welt

```mcfunction
dimensionbridge transfer @p[distance=..3,limit=1,sort=nearest] vanilla
```

### Rückkehrzelle auf jedem Spielserver

```mcfunction
dimensionbridge transfer @p[distance=..3,limit=1,sort=nearest] lobby
```

Die jeweilige Fabric-Konfiguration muss das Ziel ebenfalls erlauben.

---

## 13. Beispiel: Flimmer-Effekt mit Countdown

Einmalig ausführen:

```mcfunction
scoreboard objectives add dimensionTarget dummy
scoreboard objectives add dimensionTimer dummy
```

### Startknopf für die Hauptwelt

Impulsblock:

```mcfunction
scoreboard players set @p[distance=..3,limit=1,sort=nearest] dimensionTarget 1
```

Anschließender Kettenblock:

```mcfunction
scoreboard players set @p[distance=..3,limit=1,sort=nearest] dimensionTimer 60
```

60 Ticks entsprechen ungefähr drei Sekunden.

### Wiederholender Effektblock

Wiederholend, unbedingt, immer aktiv:

```mcfunction
execute as @a[scores={dimensionTimer=1..}] at @s run particle minecraft:portal ~ ~1 ~ 0.3 0.8 0.3 0.1 10 force
```

Anschließende Kettenblöcke, alle immer aktiv:

```mcfunction
execute as @a[scores={dimensionTimer=1..}] at @s run particle minecraft:reverse_portal ~ ~1 ~ 0.45 0.9 0.45 0.08 5 force
```

```mcfunction
execute as @a[scores={dimensionTimer=1..}] run title @s actionbar {"text":"Dimension wird geöffnet …","color":"aqua","italic":true}
```

```mcfunction
execute as @a[scores={dimensionTimer=60}] at @s run playsound minecraft:block.beacon.activate master @s ~ ~ ~ 0.8 0.7
```

```mcfunction
execute as @a[scores={dimensionTimer=20}] at @s run playsound minecraft:block.respawn_anchor.charge master @s ~ ~ ~ 0.8 1.3
```

Der Transfer muss vor dem Herunterzählen bei Timer 1 stehen:

```mcfunction
dimensionbridge transfer @a[scores={dimensionTimer=1,dimensionTarget=1}] hauptwelt
```

Danach:

```mcfunction
scoreboard players remove @a[scores={dimensionTimer=2..}] dimensionTimer 1
```

```mcfunction
scoreboard players reset @a[scores={dimensionTimer=1}] dimensionTarget
```

```mcfunction
scoreboard players reset @a[scores={dimensionTimer=1}] dimensionTimer
```

---

## 14. Beispiel: Klickbares Chatmenü mit `/trigger`

Einmalig:

```mcfunction
scoreboard objectives add dimension trigger
scoreboard objectives add dimensionTarget dummy
scoreboard objectives add dimensionTimer dummy
```

Bevor das Menü angezeigt wird, muss das Trigger-Ziel für den Spieler aktiviert werden:

```mcfunction
scoreboard players enable @p[distance=..3,limit=1,sort=nearest] dimension
```

Beispiel für die Textkomponenten-Syntax einer nativen 1.20.1-Lobby:

```mcfunction
tellraw @p[distance=..3,limit=1,sort=nearest] [{"text":"☎ Dimensionswahl\n","color":"gold","bold":true},{"text":"[ Hauptwelt ]","color":"green","clickEvent":{"action":"run_command","value":"/trigger dimension set 1"}},{"text":"\n"},{"text":"[ Hardcore ]","color":"red","clickEvent":{"action":"run_command","value":"/trigger dimension set 2"}},{"text":"\n"},{"text":"[ Vanilla ]","color":"aqua","clickEvent":{"action":"run_command","value":"/trigger dimension set 3"}}]
```

Die JSON-Textkomponenten können sich zwischen Minecraft-Versionen ändern. Maßgeblich ist die native Version des Servers, auf dem der Commandblock läuft.

Auswahl übernehmen:

```mcfunction
execute as @a[scores={dimension=1}] run scoreboard players set @s dimensionTarget 1
```

```mcfunction
execute as @a[scores={dimension=2}] run scoreboard players set @s dimensionTarget 2
```

```mcfunction
execute as @a[scores={dimension=3}] run scoreboard players set @s dimensionTarget 3
```

```mcfunction
scoreboard players set @a[scores={dimension=1..3}] dimensionTimer 60
```

```mcfunction
scoreboard players reset @a[scores={dimension=1..3}] dimension
```

Transfers bei Timer 1:

```mcfunction
dimensionbridge transfer @a[scores={dimensionTimer=1,dimensionTarget=1}] hauptwelt
```

```mcfunction
dimensionbridge transfer @a[scores={dimensionTimer=1,dimensionTarget=2}] hardcore
```

```mcfunction
dimensionbridge transfer @a[scores={dimensionTimer=1,dimensionTarget=3}] vanilla
```

---

## 15. Fehlerdiagnose

### Der Commandblock reagiert überhaupt nicht

Prüfen:

```properties
enable-command-block=true
```

Außerdem:

- Commandblock auf **Impuls**, **Unbedingt**, **Benötigt Redstone** stellen;
- Knopf korrekt anbringen;
- „Vorherige Ausgabe“ im Commandblock aktivieren und lesen;
- zum Test den Selektor vereinfachen:

```mcfunction
dimensionbridge transfer @p hauptwelt
```

### „Unknown or incomplete command“

Mögliche Ursachen:

- falsches Fabric-JAR für die native Minecraft-Version;
- DimensionBridge nicht geladen;
- Fabric API fehlt;
- Server wurde nach Installation nicht vollständig neu gestartet.

### „Ziel ist lokal nicht freigegeben“

Das Ziel fehlt in:

```text
config/dimensionbridge-fabric.properties
```

Beispiel:

```properties
allowed-destinations=hauptwelt,hardcore,vanilla
```

Danach Backend neu starten.

### „Unbekannte Dimensionskennung bzw. unbekanntes Ziel“

Das Ziel fehlt in der Velocity-Einstellung `destinations` oder die Detaildefinition fehlt.

### „Der Zielserver ist im Proxy nicht registriert“

Der Servername stimmt nicht exakt mit `velocity.toml` überein.

### „Dieses Dimensionsportal darf dieses Ziel nicht anwählen“

Der aktuelle Backend-Name fehlt bei `destination.<ziel>.allowed-sources`.

### Versionsfehler

Mit folgendem Befehl das tatsächlich erkannte Protokoll prüfen:

```text
/dimensionbridge protocol <Spielername>
```

Den angezeigten Konstantennamen anschließend bei `allowed-protocols` verwenden.

### Direkter `/server`-Befehl wird blockiert

Das ist bei `protected=true` beabsichtigt. Benutze das Portal oder erteile einem Administrator bewusst `dimensionbridge.bypass`.

### Konfigurationsänderung greift nicht

- Velocity-Konfiguration: `/dimensionbridge reload`
- Fabric-Konfiguration: vollständiger Backend-Neustart

### Keine Payload erreicht Velocity

Prüfen:

- Spieler ist zum Zeitpunkt des Befehls tatsächlich mit dem sendenden Backend verbunden;
- auf Velocity und Backend läuft dieselbe DimensionBridge-Generation;
- Kanal ist auf beiden Seiten `dimensionbridge:transfer`;
- keine alte TelephoneBridge-/DimensionBridge-JAR liegt zusätzlich im Plugin- oder Mod-Ordner.

---

## 16. Empfohlene Sicherheitskonfiguration

Velocity:

```properties
deny-unlisted-targets=true
```

Alle Spielwelten:

```properties
destination.hauptwelt.protected=true
destination.hardcore.protected=true
destination.vanilla.protected=true
```

Lobby:

```properties
destination.lobby.protected=false
```

LuckPerms:

```text
/lp group default permission set velocity.command.server false
```

Fabric-Positivlisten möglichst eng halten:

```properties
# Lobby
allowed-destinations=hauptwelt,hardcore,vanilla
```

```properties
# Spielwelten
allowed-destinations=lobby
```

---

## 17. Aktualisierung

1. Proxy und Backends stoppen.
2. Alte DimensionBridge-JAR entfernen.
3. Neue Velocity-JAR installieren.
4. Auf jedem Backend das JAR für die exakte native Minecraft-Version installieren.
5. Keine zwei DimensionBridge-Versionen gleichzeitig laden.
6. Konfigurationsdateien sichern.
7. Proxy und Backends neu starten.
8. `/dimensionbridge info` und einen einfachen Commandblock-Transfer testen.

---

## 18. Hinweise für Modrinth und CurseForge

- Fabric-JARs werden als **serverseitiger Fabric-Mod** veröffentlicht.
- Client-Unterstützung: nicht erforderlich beziehungsweise nicht unterstützt.
- Server-Unterstützung: erforderlich.
- Fabric API als erforderliche Abhängigkeit angeben.
- Für jede Minecraft-Version das dazu passende JAR bereitstellen.
- Das Velocity-JAR sollte als separates Proxy-Plugin-Projekt veröffentlicht werden und nicht mit den Fabric-Dateien im selben Projekt vermischt werden.
