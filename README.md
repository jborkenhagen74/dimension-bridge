# DimensionBridge 1.2.2

DimensionBridge ist eine rein serverseitige, abgesicherte Bridge zwischen Fabric-Backends und einem Velocity-Proxy. Commandblöcke können Spieler zu registrierten Velocity-Servern verbinden, ohne dass normale Spieler `/server` oder eine Bridge-Berechtigung erhalten.

## Artefakte

Das Projekt baut:

- ein Velocity-Plugin für den aktuellen Velocity-4.1-API-Zweig;
- ein separates Fabric-Backend-JAR für jede stabile Minecraft-Version von 1.20.1 bis 26.2.

Es ist kein Client-Mod erforderlich.

## Unterstützte Fabric-Versionen

Der Build enthält 22 stabile Ziele:

```text
1.20.1  1.20.2  1.20.3  1.20.4  1.20.5  1.20.6
1.21    1.21.1  1.21.2  1.21.3  1.21.4  1.21.5
1.21.6  1.21.7  1.21.8  1.21.9  1.21.10 1.21.11
26.1    26.1.1  26.1.2  26.2
```

Snapshots, Pre-Releases und Release Candidates sind bewusst ausgeschlossen. Details stehen in [SUPPORTED_VERSIONS.md](SUPPORTED_VERSIONS.md).

## Wartbare Multi-Version-Architektur

Die 22 JARs verwenden nicht 22 getrennte Codekopien, sondern fünf API-Familien:

| Familie | Minecraft | Java | Netzwerk-API |
|---|---|---:|---|
| `legacy` | 1.20.1–1.20.4 | 17 | `Identifier/ResourceLocation` + `PacketByteBuf/FriendlyByteBuf` |
| `typed-constructor` | 1.20.5–1.20.6 | 21 | typisierte Payloads; `ResourceLocation` wird noch über den öffentlichen Konstruktor erstellt |
| `typed` | 1.21–1.21.10 | 21 | typisierte `CustomPacketPayload`-Pakete mit `ResourceLocation.fromNamespaceAndPath(...)` |
| `typed-identifier` | 1.21.11 | 21 | typisierte Payloads nach der Umbenennung zu `Identifier` |
| `unobfuscated` | 26.1–26.2 | 25 | nicht remappendes Loom und neue Mojang-Namen |

Die gemeinsame Konfigurations- und Validierungslogik liegt in `fabric-common`. Versionsabhängiger Minecraft-/Fabric-Code liegt in `fabric-families`.

## Voraussetzungen

### Gesamter Build

- JDK 25
- Gradle 9.5.1
- Internetzugriff auf Fabric Maven, Mojang und PaperMC

Der Build läuft vollständig mit einer JDK-25-Toolchain. `javac --release` erzeugt für die älteren Zielversionen dennoch Java-17- beziehungsweise Java-21-kompatible Klassen; zusätzliche lokal installierte JDKs sind nicht erforderlich.

### Laufzeit

- Minecraft 1.20.1–1.20.4: Java 17
- Minecraft 1.20.5–1.21.11: Java 21
- Minecraft 26.1–26.2: Java 25
- Fabric Loader und Fabric API passend zur jeweiligen Spielversion
- aktueller Velocity-Proxy für das Velocity-Artefakt

## Bauen

### Alles bauen und einsammeln

```bash
gradle clean collectReleaseArtifacts
```

oder:

```bash
./build.sh
```

Alle normalen Release-JARs landen anschließend zusätzlich in:

```text
build/releases/
```

### Nur ein einzelnes Fabric-Ziel bauen

```bash
gradle :fabric-1.21.4:build
```

### Mehrere ausgewählte Ziele bauen

```bash
gradle buildSelected -PmcVersions=1.20.1,1.21.1,26.2
```

### Alle Fabric-Ziele ohne Velocity bauen

```bash
gradle buildFabricAll
```

### Versionsmatrix anzeigen

```bash
gradle printSupportedVersions
```

### Matrix prüfen und je API-Familie einen Vertreter bauen

```bash
gradle verifyVersionMatrix
gradle buildFamilyRepresentatives
```

`buildFamilyRepresentatives` baut Velocity sowie 1.20.1, 1.20.5, 1.21, 1.21.11 und 26.2. Das ist der schnelle Kompatibilitätstest vor einem vollständigen 22-Versionen-Build.

## Spätere Minecraft-Versionen ergänzen

Neue Ziele lassen sich ohne Kopieren des gesamten Mods ergänzen. Wenn die neue Version zu einer vorhandenen API-Familie passt:

```bash
python3 tools/add-fabric-target.py 26.3 unobfuscated 25 plain
gradle verifyVersionMatrix
gradle :fabric-26.3:build
```

Weicht die Minecraft-/Fabric-API ab, wird zuerst eine neue kleine Adapterfamilie unter `fabric-families/` angelegt. Gemeinsame Konfiguration und Sicherheitslogik bleiben unverändert.

## Fabric-API-Versionen

Für 1.20.1, 1.21.11, 26.1.2 und 26.2 sind bekannte API-Versionen in `gradle.properties` festgesetzt. Für alle anderen Ziele wählt Gradle automatisch die neueste Maven-Version von Fabric API, deren Versionsname auf `+<Minecraft-Version>` endet.

Nach einem erfolgreichen Gesamtbuild sollte die Auswahl fixiert werden:

```bash
gradle buildFabricAll --write-locks
```

Die erzeugten Lockfiles sollten ins Repository übernommen werden. Dadurch bleiben spätere Builds reproduzierbar, obwohl die Fallback-Auflösung dynamisch ist.

Eine Version kann jederzeit explizit überschrieben werden:

```properties
fabric_api_1_21_4=0.119.4+1.21.4
```

Das Schema lautet `fabric_api_<Version mit Unterstrichen>`.

## Erwartete Dateinamen

Beispiele:

```text
velocity/build/libs/dimensionbridge-velocity-1.2.2.jar
fabric-versions/1.20.1/build/libs/dimensionbridge-fabric-1.20.1-1.2.2+mc1.20.1.jar
fabric-versions/1.21.11/build/libs/dimensionbridge-fabric-1.21.11-1.2.2+mc1.21.11.jar
fabric-versions/26.2/build/libs/dimensionbridge-fabric-26.2-1.2.2+mc26.2.jar
```

Bei remappenden Loom-Versionen ist das normale Release-Artefakt die von `remapJar` erzeugte Datei. Entwicklungs- und Sources-JARs werden beim Einsammeln ausgeschlossen.

## Installation

1. `dimensionbridge-velocity-1.2.2.jar` in `plugins/` des Velocity-Proxys kopieren.
2. Auf jedem Fabric-Backend genau das JAR verwenden, dessen Minecraft-Version der nativen Serverversion entspricht.
3. Server einmal starten und stoppen.
4. Konfigurationen bearbeiten:
   - Velocity: `plugins/dimensionbridge/dimensionbridge.properties`
   - Fabric: `config/dimensionbridge-fabric.properties`
5. Backends und Proxy vollständig neu starten.

Alle Fabric-JARs verwenden denselben Kanal:

```text
dimensionbridge:transfer
```

Daher ist nur ein Velocity-Plugin für alle Backend-Versionen nötig.

## Commandblock

Beispiel:

```mcfunction
dimensionbridge transfer @p[distance=..3,limit=1,sort=nearest] hauptwelt
```

Der Befehl ist backendseitig nur für Commandblöcke, Konsole und entsprechend privilegierte Quellen verfügbar.

## Konfiguration

### Fabric

```properties
allowed-destinations=lobby,hauptwelt,hardcore,vanilla
```

### Velocity

```properties
initial-servers=lobby
deny-unlisted-targets=false
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
```

Die Namen müssen exakt den Einträgen in `velocity.toml` entsprechen.

## Sicherheit

1. Das Fabric-Backend akzeptiert nur lokal freigegebene Zielnamen.
2. Velocity akzeptiert die Payload nur von einer echten Backend-Verbindung.
3. Quelle, Ziel, Spieler, Protokollversion, Cooldown und Autorisierungsfenster werden geprüft.
4. Geschützte Ziele werden in `ServerPreConnectEvent` gesperrt, wenn keine gültige Bridge-Anforderung vorliegt.
5. `/server` kann normalen Spielern daher entzogen werden:

```text
/lp group default permission set velocity.command.server false
```

## Aktualisierung von 1.1.0

Die Laufzeitkonfiguration und der Messaging-Kanal bleiben unverändert. Für ein bestehendes Setup genügt es daher, die Fabric-JAR auf jedem Backend durch die zur nativen Minecraft-Version passende 1.2.2-JAR und das Velocity-JAR durch Version 1.2.2 zu ersetzen. Alte und neue Fabric-JARs dürfen nicht gleichzeitig im selben `mods`-Ordner liegen.


### GitHub Actions

Enthalten sind:

- `.github/workflows/build.yml`
- `.github/workflows/publish.yml`

Für den Publish-Workflow werden benötigt:

**Repository Secrets**

```text
MODRINTH_TOKEN
CURSEFORGE_TOKEN
```

**Repository Variables**

```text
MODRINTH_FABRIC_PROJECT_ID
CURSEFORGE_FABRIC_PROJECT_ID
```


## Projektstruktur

```text
gradle/fabric-targets.json       stabile Zielversionen
gradle/fabric-version.gradle    gemeinsamer Build aller Fabric-Ziele
fabric-common/                  gemeinsame Konfiguration/Validierung
fabric-families/legacy/         1.20.1–1.20.4
fabric-families/typed-constructor/ 1.20.5–1.20.6
fabric-families/typed/          1.21–1.21.10
fabric-families/typed-identifier/ 1.21.11
fabric-families/unobfuscated/   26.1–26.2
fabric-versions/<version>/      kleine Gradle-Subprojekte
velocity/                       Velocity-Plugin
```

## Lizenz

MIT
