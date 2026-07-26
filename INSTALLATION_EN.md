# DimensionBridge – Installation, Configuration and Command Guide

## 1. Overview

DimensionBridge is a server-side-only bridge between Fabric backend servers and a Velocity proxy. It allows command blocks, the server console and suitably privileged command sources to transfer players to a registered Velocity server.

Players do not need access to Velocity's `/server` command or to any bridge permission. Protected destination servers can therefore be made accessible only through approved portals, telephone booths or other command-block contraptions.

### Architecture

```text
Minecraft client
       │
       ▼
Velocity proxy
└─ DimensionBridge Velocity plugin
       │  Channel: dimensionbridge:transfer
       ▼
Fabric backend
└─ DimensionBridge Fabric mod
   └─ Command block: dimensionbridge transfer <targets> <server>
```

No client-side mod is required.

---

## 2. Supported Versions

A separate JAR is built for every native Fabric server version:

```text
1.20.1  1.20.2  1.20.3  1.20.4  1.20.5  1.20.6
1.21    1.21.1  1.21.2  1.21.3  1.21.4  1.21.5
1.21.6  1.21.7  1.21.8  1.21.9  1.21.10 1.21.11
26.1    26.1.1  26.1.2  26.2
```

Always use the Fabric JAR that exactly matches the **native version of that backend server**. The client version translated through ViaVersion is not relevant when selecting the backend JAR.

### Runtime Java versions

| Minecraft version | Java |
|---|---:|
| 1.20.1–1.20.4 | 17 |
| 1.20.5–1.21.11 | 21 |
| 26.1–26.2 | 25 |
| current Velocity plugin | 25 |

---

## 3. Requirements

### Velocity

- a Velocity proxy;
- Java 25;
- every backend registered in `velocity.toml`;
- optionally LuckPerms for permission management.

### Fabric backends

- Fabric Loader matching the Minecraft version;
- Fabric API matching the Minecraft version;
- the matching DimensionBridge Fabric JAR;
- command blocks enabled in `server.properties`:

```properties
enable-command-block=true
```

A full server restart is required after changing `server.properties`.

---

## 4. Downloading GitHub Actions Artifacts

After a successful build:

1. Open the GitHub repository.
2. Select **Actions**.
3. Open the successful build workflow run.
4. Scroll to **Artifacts**.
5. Download `dimensionbridge-all`.
6. Extract the ZIP file.

Use only normal release JARs. Files ending in `-sources.jar` or `-dev.jar` are not server installation files.

---

## 5. Installing the Velocity Plugin

1. Stop Velocity completely.
2. Copy the Velocity JAR into:

```text
velocity/plugins/dimensionbridge-velocity-<VERSION>.jar
```

3. Start Velocity.
4. The first start creates:

```text
plugins/dimensionbridge/dimensionbridge.properties
```

5. Edit the file. You can then reload it with `/dimensionbridge reload`.

The proxy console should contain a message similar to:

```text
DimensionBridge loaded. Channel: dimensionbridge:transfer
```

---

## 6. Installing a Fabric Backend Mod

Repeat these steps for every backend server:

1. Stop the backend completely.
2. Copy the exact matching Fabric JAR into the `mods` directory.
3. Ensure that Fabric API is installed.
4. Keep only **one** DimensionBridge JAR in the `mods` directory.
5. Start the backend.
6. The first start creates:

```text
config/dimensionbridge-fabric.properties
```

7. Edit the file and restart the backend.

The backend console should contain a message similar to:

```text
DimensionBridge backend <VERSION> loaded. Channel: dimensionbridge:transfer
```

The Fabric configuration currently has no reload command. Changes to `dimensionbridge-fabric.properties` require a backend restart.

---

## 7. Fabric Configuration

File:

```text
config/dimensionbridge-fabric.properties
```

Lobby example:

```properties
allowed-destinations=hauptwelt,hardcore,vanilla
```

Game-server example, allowing only a return to the lobby:

```properties
allowed-destinations=lobby
```

Allow every syntactically valid target:

```properties
allowed-destinations=*
```

### `allowed-destinations`

This is the local allowlist of the individual Fabric backend. A transfer payload is sent to Velocity only when the requested target is allowed here.

- separate multiple names with commas;
- names are normalized to lowercase;
- names may contain lowercase letters, numbers, `_` and `-`;
- `*` allows every syntactically valid destination that Velocity subsequently accepts.

For security, use a narrow allowlist instead of `*` whenever possible.

---

## 8. Velocity Configuration

File:

```text
plugins/dimensionbridge/dimensionbridge.properties
```

Complete example using the servers `lobby`, `hauptwelt`, `hardcore` and `vanilla`:

```properties
# Server names must exactly match velocity.toml.
# * allows every source or protocol.

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

destination.hauptwelt.display-name=Main World
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

### Global settings

#### `initial-servers`

Comma-separated list of servers permitted as initial proxy login servers.

```properties
initial-servers=lobby
```

#### `deny-unlisted-targets`

```properties
deny-unlisted-targets=true
```

- `true`: servers not listed under `destinations` are denied;
- `false`: unlisted servers remain outside DimensionBridge's protection rules.

Use `true` for a closed portal network.

#### `cooldown-ms`

Delay between two bridge requests from the same player:

```properties
cooldown-ms=2500
```

Allowed range: 0 to 60,000 milliseconds.

#### `authorization-window-ms`

Time window during which the Velocity connection request started by DimensionBridge remains authorized:

```properties
authorization-window-ms=5000
```

Allowed range: 500 to 60,000 milliseconds. The value should be longer than a normal proxy-to-backend connection attempt.

#### `bypass-permission`

Permission that bypasses all DimensionBridge protection checks:

```properties
bypass-permission=dimensionbridge.bypass
```

Grant it only to trusted administrators.

### Destination definitions

Every entry in `destinations` needs four properties:

```properties
destinations=lobby,hauptwelt

destination.hauptwelt.display-name=Main World
destination.hauptwelt.protected=true
destination.hauptwelt.allowed-sources=lobby
destination.hauptwelt.allowed-protocols=MINECRAFT_1_20
```

#### `display-name`

Human-readable name used in player messages.

#### `protected`

- `true`: entering the target requires a valid DimensionBridge authorization. Direct `/server` connections are denied unless the player has the bypass permission.
- `false`: normal connections are allowed when the protocol rule matches.

The lobby should normally use `protected=false` so initial login and return travel remain reliable.

#### `allowed-sources`

Backend servers from which this destination may be requested:

```properties
destination.hauptwelt.allowed-sources=lobby
```

Multiple sources:

```properties
destination.hauptwelt.allowed-sources=lobby,hardcore
```

Every source:

```properties
destination.lobby.allowed-sources=*
```

#### `allowed-protocols`

Velocity client protocol names accepted by this destination:

```properties
destination.hauptwelt.allowed-protocols=MINECRAFT_1_20
```

Multiple protocols:

```properties
destination.hauptwelt.allowed-protocols=MINECRAFT_1_20,MINECRAFT_1_21
```

Every protocol:

```properties
destination.lobby.allowed-protocols=*
```

Minecraft 1.20 and 1.20.1 share the `MINECRAFT_1_20` protocol group.

Use this command to obtain the exact Velocity name for a connected client:

```text
/dimensionbridge protocol <player>
```

---

## 9. Permissions

### `dimensionbridge.admin`

Allows a player to use DimensionBridge's administrative Velocity commands.

### `dimensionbridge.bypass`

Bypasses destination, source, protocol and portal-authorization checks. Grant it only to trusted administrators.

### `velocity.command.server`

Controls Velocity's normal `/server` command. It can be denied to regular players when transfers should only happen through portals:

```text
/lp group default permission set velocity.command.server false
```

Administrators may retain it:

```text
/lp group admin permission set velocity.command.server true
```

DimensionBridge does not require `velocity.command.server`.

### Backend command permission

`dimensionbridge transfer` is available on Fabric only to the console, command blocks and sources with moderator/command level 2. Regular players cannot use it. Do not grant normal players operator or moderator rights.

---

## 10. Command Reference

### Fabric backend

#### Transfer players

```text
/dimensionbridge transfer <targets> <Velocity server name>
```

Omit the leading slash in command blocks:

```mcfunction
dimensionbridge transfer @p hauptwelt
```

Examples:

```mcfunction
# Nearest player
dimensionbridge transfer @p hauptwelt

# Nearest player within three blocks
dimensionbridge transfer @p[distance=..3,limit=1,sort=nearest] hauptwelt

# Every player inside a one-block-wide, three-block-high booth
dimensionbridge transfer @a[x=10,y=64,z=5,dx=0,dy=2,dz=0] hauptwelt

# Only players not tagged as being in combat
dimensionbridge transfer @a[x=10,y=64,z=5,distance=..2,tag=!inCombat] hauptwelt

# Return to the lobby
dimensionbridge transfer @p[distance=..3,limit=1,sort=nearest] lobby
```

### Velocity

#### Help

```text
/dimensionbridge
/db
```

#### Reload Velocity configuration

```text
/dimensionbridge reload
/db reload
```

This reloads only the Velocity configuration.

#### Show active configuration

```text
/dimensionbridge info
/db info
```

The output displays the configuration path and loaded destinations.

#### Show a client protocol

```text
/dimensionbridge protocol
/dimensionbridge protocol <player>
```

When a player omits the name, the command displays that player's own protocol.

---

## 11. Basic Functional Test

Command block settings:

```text
Impulse
Unconditional
Needs Redstone
```

Command:

```mcfunction
dimensionbridge transfer @p[distance=..3,limit=1,sort=nearest] hauptwelt
```

Stand no more than three blocks from the command block and press the button.

To eliminate the radius as a possible cause:

```mcfunction
dimensionbridge transfer @p hauptwelt
```

The command block's previous output should be similar to:

```text
DimensionBridge: sent 1 transfer request(s) to 'hauptwelt'.
```

The current mod may display this message in German; the number and destination are the important parts.

---

## 12. Example: Lobby Telephone Booths

### Booth to the main world

```mcfunction
dimensionbridge transfer @p[distance=..3,limit=1,sort=nearest] hauptwelt
```

### Booth to the hardcore world

```mcfunction
dimensionbridge transfer @p[distance=..3,limit=1,sort=nearest] hardcore
```

### Booth to the vanilla world

```mcfunction
dimensionbridge transfer @p[distance=..3,limit=1,sort=nearest] vanilla
```

### Return booth on every game server

```mcfunction
dimensionbridge transfer @p[distance=..3,limit=1,sort=nearest] lobby
```

The Fabric allowlist on that backend must also permit the destination.

---

## 13. Example: Shimmer Effect and Countdown

Run once:

```mcfunction
scoreboard objectives add dimensionTarget dummy
scoreboard objectives add dimensionTimer dummy
```

### Main-world start button

Impulse block:

```mcfunction
scoreboard players set @p[distance=..3,limit=1,sort=nearest] dimensionTarget 1
```

Following chain block:

```mcfunction
scoreboard players set @p[distance=..3,limit=1,sort=nearest] dimensionTimer 60
```

60 ticks are approximately three seconds.

### Repeating effect block

Repeating, unconditional, always active:

```mcfunction
execute as @a[scores={dimensionTimer=1..}] at @s run particle minecraft:portal ~ ~1 ~ 0.3 0.8 0.3 0.1 10 force
```

Following chain blocks, all always active:

```mcfunction
execute as @a[scores={dimensionTimer=1..}] at @s run particle minecraft:reverse_portal ~ ~1 ~ 0.45 0.9 0.45 0.08 5 force
```

```mcfunction
execute as @a[scores={dimensionTimer=1..}] run title @s actionbar {"text":"Opening dimension …","color":"aqua","italic":true}
```

```mcfunction
execute as @a[scores={dimensionTimer=60}] at @s run playsound minecraft:block.beacon.activate master @s ~ ~ ~ 0.8 0.7
```

```mcfunction
execute as @a[scores={dimensionTimer=20}] at @s run playsound minecraft:block.respawn_anchor.charge master @s ~ ~ ~ 0.8 1.3
```

The transfer must run before timer value 1 is removed:

```mcfunction
dimensionbridge transfer @a[scores={dimensionTimer=1,dimensionTarget=1}] hauptwelt
```

Then:

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

## 14. Example: Clickable Chat Menu with `/trigger`

Run once:

```mcfunction
scoreboard objectives add dimension trigger
scoreboard objectives add dimensionTarget dummy
scoreboard objectives add dimensionTimer dummy
```

Enable the trigger for the player before displaying the menu:

```mcfunction
scoreboard players enable @p[distance=..3,limit=1,sort=nearest] dimension
```

Example using the text-component syntax of a native Minecraft 1.20.1 lobby:

```mcfunction
tellraw @p[distance=..3,limit=1,sort=nearest] [{"text":"☎ Select dimension\n","color":"gold","bold":true},{"text":"[ Main World ]","color":"green","clickEvent":{"action":"run_command","value":"/trigger dimension set 1"}},{"text":"\n"},{"text":"[ Hardcore ]","color":"red","clickEvent":{"action":"run_command","value":"/trigger dimension set 2"}},{"text":"\n"},{"text":"[ Vanilla ]","color":"aqua","clickEvent":{"action":"run_command","value":"/trigger dimension set 3"}}]
```

JSON text-component syntax may change between Minecraft releases. Use the syntax supported by the native version of the server executing the command block.

Store the selected target:

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

Transfers at timer value 1:

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

## 15. Troubleshooting

### The command block does nothing

Verify:

```properties
enable-command-block=true
```

Also:

- set the block to **Impulse**, **Unconditional**, **Needs Redstone**;
- ensure that the button powers the block;
- enable and read the command block's previous output;
- simplify the selector for testing:

```mcfunction
dimensionbridge transfer @p hauptwelt
```

### “Unknown or incomplete command”

Possible causes:

- wrong Fabric JAR for the native Minecraft version;
- DimensionBridge did not load;
- Fabric API is missing;
- the backend was not fully restarted after installation.

### “Target is not locally allowed”

The destination is missing from:

```text
config/dimensionbridge-fabric.properties
```

Example:

```properties
allowed-destinations=hauptwelt,hardcore,vanilla
```

Restart the backend afterwards.

### “Unknown dimension identifier or destination”

The destination is missing from the Velocity `destinations` list or its destination property block is missing.

### “Destination server is not registered in the proxy”

The server name does not exactly match its entry in `velocity.toml`.

### “This portal may not select this destination”

The current backend is missing from `destination.<target>.allowed-sources`.

### Version denied

Check the actual client protocol:

```text
/dimensionbridge protocol <player>
```

Use the displayed constant in `allowed-protocols`.

### Direct `/server` is denied

This is expected for targets with `protected=true`. Use the portal or deliberately grant `dimensionbridge.bypass` to an administrator.

### Configuration changes are not applied

- Velocity configuration: `/dimensionbridge reload`
- Fabric configuration: full backend restart

### No payload reaches Velocity

Verify that:

- the selected player is currently connected to the sending backend;
- matching DimensionBridge generations are installed on proxy and backend;
- both sides use the channel `dimensionbridge:transfer`;
- no old TelephoneBridge or duplicate DimensionBridge JAR remains installed.

---

## 16. Recommended Security Configuration

Velocity:

```properties
deny-unlisted-targets=true
```

Game worlds:

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

Keep Fabric allowlists narrow:

```properties
# Lobby
allowed-destinations=hauptwelt,hardcore,vanilla
```

```properties
# Game worlds
allowed-destinations=lobby
```

---

## 17. Updating

1. Stop the proxy and all backends.
2. Remove the old DimensionBridge JARs.
3. Install the new Velocity JAR.
4. Install the JAR matching each backend's exact native Minecraft version.
5. Never load two DimensionBridge versions at the same time.
6. Back up the configuration files.
7. Restart proxy and backends.
8. Test `/dimensionbridge info` and one basic command-block transfer.

---

## 18. Modrinth and CurseForge Notes

- Publish Fabric JARs as a **server-side Fabric mod**.
- Client support: unsupported/not required.
- Server support: required.
- Mark Fabric API as a required dependency.
- Attach the matching JAR to each supported Minecraft version.
- Publish the Velocity JAR as a separate proxy-plugin project rather than mixing it with the Fabric files in the same project.
