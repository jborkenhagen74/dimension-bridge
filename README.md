# DimensionBridge

Rein serverseitige, abgesicherte Dimensions-Bridge für ein Minecraft-Netzwerk hinter Velocity.

Das Projekt enthält drei Artefakte:

- `dimensionbridge-velocity`: Plugin für den aktuellen Velocity-4.1-API-Zweig
- `dimensionbridge-fabric-1.20.1`: serverseitiger Fabric-Mod für Minecraft 1.20.1
- `dimensionbridge-fabric-26.2`: serverseitiger Fabric-Mod für Minecraft 26.2

Es ist **kein Client-Mod** erforderlich. Die Fabric-Backends senden eine Custom-Payload an Velocity; Velocity fängt sie ab, prüft Route und Clientprotokoll und verbindet den Spieler direkt über seine API. Der Spielerbefehl `/server` wird dabei nicht benutzt.

## Sicherheitsmodell

1. `/dimensionbridge transfer` ist backendseitig nur für Befehlsblöcke, Konsole und Moderatoren/OPs registriert.
2. Der Fabric-Mod akzeptiert nur lokal freigegebene Zielnamen.
3. Velocity akzeptiert Nachrichten nur von einer echten `ServerConnection`.
4. Velocity prüft, ob der Spieler noch auf genau diesem sendenden Backend ist.
5. Ziel, erlaubter Quellserver und Clientprotokoll werden erneut auf Velocity geprüft.
6. Geschützte Ziele werden in `ServerPreConnectEvent` blockiert, sofern unmittelbar zuvor keine gültige Bridge-Anforderung vorlag. Damit scheitern auch manuelles `/server`, andere Direktbefehle oder einfache Menü-Plugins.
7. Ein kurzer Cooldown verhindert Spam und Doppeltransfers.

## Voraussetzungen

### Proxy

- aktueller Velocity-Build mit API 4.1
- Java 25 für den aktuellen Velocity-4.1-Snapshot
- optional LuckPerms, um `/server` für Spieler zu entziehen

### Minecraft 1.20.1 Backend

- Fabric Loader 0.19.3
- Fabric API 0.92.11+1.20.1
- Java 17

### Minecraft 26.2 Backend

- Fabric Loader 0.19.3
- Fabric API 0.155.2+26.2
- Java 25

Vorhandene Mods wie FabricProxy-Lite, CrossStitch und ViaVersion können parallel weiterlaufen. Proxy Command wird für DimensionBridge nicht benötigt.

## Bauen

Für den vollständigen Build wird ein JDK 25 sowie Gradle 9.5.1 empfohlen. Gradle verwendet für die einzelnen Module passende Release-Ziele.

Das Projekt verwendet Fabric Loom `1.17.16` für beide Loom-Plugin-IDs. Beim nicht verschleierten Minecraft 26.2 werden Fabric Loader und Fabric API über Gradles `implementation`-Konfiguration eingebunden; `modImplementation` existiert in diesem Loom-Modus nicht.

```bash
gradle clean buildAll
```

oder unter Linux/macOS:

```bash
./build.sh
```

Ergebnisse:

```text
velocity/build/libs/dimensionbridge-velocity-1.1.0.jar
fabric-1.20.1/build/libs/dimensionbridge-fabric-1.20.1-1.1.0.jar
fabric-26.2/build/libs/dimensionbridge-fabric-26.2-1.1.0.jar
```

## Fehlerbehebung beim Build

### Velocity-4.1-Snapshot verlangt Java 25

Aktuelle Veröffentlichungen von `com.velocitypowered:velocity-api:4.1.0-SNAPSHOT` sind für Java 25 gebaut. Das Velocity-Modul verwendet deshalb eine Java-25-Toolchain und `options.release = 25`. Der Proxy muss dieses Plugin ebenfalls mit Java 25 laden.

Falls Gradle meldet, die Velocity-API sei nur mit JVM 25 oder neuer kompatibel, prüfe `velocity/build.gradle`:

```gradle
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
}

tasks.withType(JavaCompile).configureEach {
    options.release = 25
    options.encoding = 'UTF-8'
}
```

Falls ein älterer Projektstand diesen Fehler meldet:

```text
Could not find method modImplementation() ... in project ':fabric-26.2'
```

prüfe `fabric-26.2/build.gradle`. Für Minecraft 26.2 muss der Abhängigkeitsblock so aussehen:

```gradle
dependencies {
    minecraft 'com.mojang:minecraft:26.2'
    implementation 'net.fabricmc:fabric-loader:0.19.3'
    implementation 'net.fabricmc.fabric-api:fabric-api:0.155.2+26.2'
}
```

Danach den Gradle-Cache für dieses Projekt neu auswerten:

```bash
gradle --stop
gradle clean buildAll --refresh-dependencies
```

## Migration von Version 1.0.x

Die Umbenennung auf DimensionBridge ändert technische IDs und Dateinamen. Vor der Installation:

1. Alte Bridge-JARs aus `plugins/` und `mods/` entfernen.
2. Die bisherige Velocity-Konfiguration in die neue Datei `plugins/dimensionbridge/dimensionbridge.properties` übernehmen.
3. Die bisherige Fabric-Positivliste in `config/dimensionbridge-fabric.properties` übernehmen.
4. Alle bisherigen Commandblöcke auf `dimensionbridge transfer ...` umstellen.
5. LuckPerms-Knoten auf `dimensionbridge.admin` und `dimensionbridge.bypass` umstellen.
6. Proxy und Backends vollständig neu starten.

Alte und neue Bridge-Versionen dürfen nicht gleichzeitig installiert sein, da sie unterschiedliche Plugin-Messaging-Kanäle verwenden.

## Installation

1. `dimensionbridge-velocity-1.1.0.jar` nach `plugins/` des Velocity-Proxys kopieren.
2. Den passenden Fabric-JAR nach `mods/` jedes Backends kopieren, das ein Dimensionsportal auslösen soll:
   - Lobby: passend zur nativen Lobbyversion
   - 1.20.1-Spielwelten: 1.20.1-JAR
   - 26.2-Spielwelt: 26.2-JAR
3. Proxy und Backends einmal starten und wieder stoppen.
4. Die erzeugten Konfigurationen bearbeiten:
   - Velocity: `plugins/dimensionbridge/dimensionbridge.properties`
   - Fabric: `config/dimensionbridge-fabric.properties`
5. Alle Servernamen exakt an die Namen aus `[servers]` in `velocity.toml` anpassen.
6. Erst die Backends und danach Velocity neu starten.

Beispieldateien liegen unter `examples/`.

## Velocity-Konfiguration

```properties
initial-servers=lobby
deny-unlisted-targets=false
cooldown-ms=2500
authorization-window-ms=5000
bypass-permission=dimensionbridge.bypass

destinations=lobby,conquest,welt2,welt262

destination.lobby.display-name=Lobby
destination.lobby.protected=false
destination.lobby.allowed-sources=*
destination.lobby.allowed-protocols=*

destination.conquest.display-name=Conquest Reforged
destination.conquest.protected=true
destination.conquest.allowed-sources=lobby
destination.conquest.allowed-protocols=MINECRAFT_1_20

destination.welt2.display-name=Zweite 1.20.1-Welt
destination.welt2.protected=true
destination.welt2.allowed-sources=lobby
destination.welt2.allowed-protocols=MINECRAFT_1_20

destination.welt262.display-name=26.2-Welt
destination.welt262.protected=true
destination.welt262.allowed-sources=lobby
destination.welt262.allowed-protocols=MINECRAFT_26_2
```

### Bedeutung

- `initial-servers`: Ziele, die beim ersten Login ohne Bridge betreten werden dürfen.
- `deny-unlisted-targets`: `true` sperrt auch alle nicht in `destinations` aufgeführten Backends.
- `protected`: Bei `true` ist eine gültige Bridge-Anforderung zwingend nötig.
- `allowed-sources`: Backend-Namen, von denen dieses Ziel angewählt werden darf.
- `allowed-protocols`: Velocity-Enum-Namen oder `*`.
- `bypass-permission`: Erlaubt Administratoren direkte Wechsel zu geschützten Zielen.

Minecraft 1.20 und 1.20.1 verwenden dasselbe Netzwerkprotokoll. Velocity kann sie daher nicht voneinander unterscheiden; beide erscheinen als `MINECRAFT_1_20`.

## Fabric-Konfiguration

```properties
allowed-destinations=lobby,conquest,welt2,welt262
```

Diese Positivliste ist eine zusätzliche lokale Sicherung. Auf einer Spielwelt kannst du beispielsweise nur `lobby` erlauben.

## Rechte

Normale Spieler benötigen **keine** DimensionBridge- oder Velocity-Berechtigung.

Mit LuckPerms auf dem Proxy sollte `/server` entzogen werden:

```text
/lp group default permission set velocity.command.server false
```

Administratoren können optional erhalten:

```text
/lp group admin permission set dimensionbridge.admin true
/lp group admin permission set dimensionbridge.bypass true
```

`dimensionbridge.bypass` umgeht die geschützten Routen vollständig und sollte nur vertrauenswürdigen Administratoren gegeben werden.

## Befehle

### Fabric-Backend

```text
/dimensionbridge transfer <Zielselektor> <Velocity-Servername>
```

Funktionstest in einem Impuls-Commandblock:

```mcfunction
dimensionbridge transfer @p[distance=..3,limit=1,sort=nearest] conquest
```

Rückkehr zur Lobby:

```mcfunction
dimensionbridge transfer @p[distance=..3,limit=1,sort=nearest] lobby
```

### Velocity

```text
/dimensionbridge reload
/dimensionbridge protocol [Spieler]
/dimensionbridge info
```

Diese Befehle benötigen `dimensionbridge.admin` oder die Proxy-Konsole.

## Flimmer-Effekt und Countdown

Eine vollständige Beispielkette liegt in:

```text
examples/command-blocks.mcfunction
```

Der Transferbefehl am Ende eines Countdowns lautet beispielsweise:

```mcfunction
dimensionbridge transfer @a[scores={dimensionTimer=1,dimensionTarget=1}] conquest
```

Die Partikel-, Sound- und Timerblöcke bleiben vollständig Vanilla. Das klickbare Chatmenü kann `/trigger dimension set 1`, `/trigger dimension set 2` und `/trigger dimension set 3` setzen; niemals `/server` oder `/dimensionbridge` direkt.

## Empfohlene Testreihenfolge

1. Als OP in der Lobby den Commandblock-Test nach `conquest` ausführen.
2. `/dimensionbridge protocol <Spieler>` auf Velocity prüfen.
3. Als normaler Spieler `/server conquest` testen: muss mangels Berechtigung oder durch den Routenschutz scheitern.
4. Den Dimensionsportal-Commandblock testen: muss funktionieren.
5. Mit einer falschen Clientversion testen: Velocity muss den Wechsel ablehnen.
6. Rückkehrzelle von jeder Spielwelt zur Lobby testen.
7. Server offline nehmen und Fehlermeldung/Fallback-Verhalten mit vConnect prüfen.

## Hinweise zu vConnect und Fallbacks

Die Lobby ist im Beispiel nicht als geschütztes Ziel markiert. Dadurch können vConnect und Velocity-Fallbacks weiterhin zur Lobby umleiten. Da normalen Spielern `velocity.command.server` entzogen wird, können sie die Lobby trotzdem nicht per `/server` anwählen. Soll selbst die Lobby ausschließlich über eine Bridge erreichbar sein, setze `destination.lobby.protected=true` und trage die erlaubten Spielserver als Quellen ein; prüfe dann aber Fallbacks besonders sorgfältig.
