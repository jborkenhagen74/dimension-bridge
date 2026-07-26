# Changelog

## 1.1.0

- Benennt das gesamte Projekt auf DimensionBridge um.
- Ändert Mod-/Plugin-ID, Paketnamen, Messaging-Kanal, Befehle, Berechtigungen, Konfigurationsdateien und Artefaktnamen auf `dimensionbridge`.
- Benennt die Beispiel-Scoreboards in `dimensionTimer` und `dimensionTarget` um.
- Diese Version ist wegen der geänderten IDs und Dateinamen nicht abwärtskompatibel zu 1.0.x.

## 1.1.0

- Stellt das Velocity-Modul auf Java 25 um.
- Behebt die Gradle-Variantenauflösung für aktuelle `velocity-api:4.1.0-SNAPSHOT`-Artefakte, die mit Java 25 veröffentlicht werden.
- Aktualisiert Artefaktnamen und Installationshinweise.

## 1.0.1

- Behebt den Gradle-Auswertungsfehler im Minecraft-26.2-Modul.
- Verwendet für Minecraft 26.2 `implementation` statt der dort nicht vorhandenen Konfiguration `modImplementation`.
- Pinnt beide Fabric-Loom-Plugin-IDs auf die gemeinsame stabile Version 1.17.16.
- Ergänzt Hinweise zur Fehlerbehebung und zum Neuaufbau des Gradle-Caches.

## 1.0.0

- Erste Version des Velocity-/Fabric-Plugin-Duos.
