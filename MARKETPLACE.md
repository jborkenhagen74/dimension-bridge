# Marketplace text

## Kurzbeschreibung (DE)

DimensionBridge verbindet Spieler sicher aus Fabric-Commandblöcken mit registrierten Velocity-Backend-Servern. Der Mod ist rein serverseitig, unterstützt versionsabhängige Ziele und benötigt keinen Client-Mod.

## Short description (EN)

DimensionBridge securely transfers players from Fabric command blocks to registered Velocity backend servers. It is server-side only, supports protocol-restricted destinations, and requires no client mod.

## Beschreibung (DE)

DimensionBridge besteht aus einem Velocity-Plugin und einem passenden serverseitigen Fabric-Mod. Ein Commandblock kann über `/dimensionbridge transfer` eine autorisierte Transferanfrage auslösen. Velocity prüft Quellserver, Zielserver, Spieler, Protokollversion, Cooldown und Autorisierungsfenster, bevor die Verbindung hergestellt wird.

Geeignet für Portale, Telefonzellen, Dimensionsreisen, Lobby-Menüs und andere immersive Serverwechsel. Normale Spieler benötigen weder `/server` noch eine Bridge-Berechtigung.

### Merkmale

- rein serverseitig;
- kein Client-Mod erforderlich;
- eigene JAR pro unterstützter Minecraft-Version;
- Velocity-Zielschutz gegen manuelle Umgehung;
- Ziel-Whitelist auf Fabric und Velocity;
- Protokoll-/Clientversionsprüfung pro Zielserver;
- Commandblock- und Datapack-freundlicher Befehl;
- Veröffentlichungsvarianten für Modrinth und CurseForge.

### Benötigt

- Fabric API auf jedem Fabric-Backend;
- das DimensionBridge-Plugin auf Velocity;
- korrektes Velocity-Player-Forwarding.

## Description (EN)

DimensionBridge consists of a Velocity plugin and a matching server-side Fabric mod. A command block can issue an authorized transfer request through `/dimensionbridge transfer`. Velocity validates the source server, destination, player, client protocol, cooldown, and authorization window before connecting the player.

It is designed for portals, dimension gates, dimension travel, lobby menus, and other immersive server switching. Regular players do not need access to `/server` or any bridge permission.
