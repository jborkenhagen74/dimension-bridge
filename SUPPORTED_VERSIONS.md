# Unterstützte Minecraft-Versionen

Für jede folgende stabile Spielversion wird ein eigenes serverseitiges Fabric-JAR gebaut:

| Minecraft | Java | Quell-/API-Familie | Ausgabe |
|---|---:|---|---|
| 1.20.1–1.20.4 | 17 | `legacy` – Identifier + PacketByteBuf | `dimensionbridge-fabric-<MC>-1.2.1+mc<MC>.jar` |
| 1.20.5–1.20.6 | 21 | `typed-constructor` – CustomPacketPayload + öffentlicher ResourceLocation-Konstruktor | `dimensionbridge-fabric-<MC>-1.2.1+mc<MC>.jar` |
| 1.21–1.21.10 | 21 | `typed` – CustomPacketPayload + ResourceLocation-Factory | `dimensionbridge-fabric-<MC>-1.2.1+mc<MC>.jar` |
| 1.21.11 | 21 | `typed-identifier` – CustomPacketPayload + Identifier | `dimensionbridge-fabric-<MC>-1.2.1+mc<MC>.jar` |
| 26.1–26.2 | 25 | `unobfuscated` – nicht remappendes Loom | `dimensionbridge-fabric-<MC>-1.2.1+mc<MC>.jar` |

Konkrete Ziele: 1.20.1, 1.20.2, 1.20.3, 1.20.4, 1.20.5, 1.20.6, 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2 und 26.2.

Snapshots, Pre-Releases und Release Candidates sind bewusst nicht Teil des regulären Release-Builds.
