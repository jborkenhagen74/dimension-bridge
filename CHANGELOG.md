# Changelog

## 1.2.0

- Fügt einen Multi-Version-Build für alle stabilen Minecraft-Versionen von 1.20.1 bis 26.2 hinzu.
- Baut für jede Spielversion ein eigenes serverseitiges Fabric-JAR.
- Teilt den Fabric-Code in vier wartbare API-Familien: Legacy, typisierte Payloads mit `ResourceLocation`, 1.21.11 mit `Identifier` und nicht verschleierte 26.x-Versionen.
- Ergänzt dynamische Fabric-API-Auflösung mit optionalen festen Overrides und Gradle Dependency Locking.
- Ergänzt Sammelaufgaben für Builds und Release-Artefakte.
- Ergänzt opt-in Veröffentlichung zu Modrinth und CurseForge über Mod Publish Plugin 2.1.1.
- Ergänzt GitHub-Actions-Workflows für Build und Veröffentlichung.

## 1.1.0

- Benennt das gesamte Projekt auf DimensionBridge um.
- Ändert Mod-/Plugin-ID, Paketnamen, Messaging-Kanal, Befehle, Berechtigungen, Konfigurationsdateien und Artefaktnamen auf `dimensionbridge`.
- Diese Version ist wegen der geänderten IDs und Dateinamen nicht abwärtskompatibel zu 1.0.x.

## 1.0.2

- Stellt das Velocity-Modul auf Java 25 um.
- Behebt die Gradle-Variantenauflösung für aktuelle `velocity-api:4.1.0-SNAPSHOT`-Artefakte.

## 1.0.1

- Behebt den Gradle-Auswertungsfehler im Minecraft-26.2-Modul.
- Verwendet für Minecraft 26.2 `implementation` statt `modImplementation`.

## 1.0.0

- Erste Version des Velocity-/Fabric-Plugin-Duos.
