package dev.dimensionbridge.velocity;

import com.velocitypowered.api.network.ProtocolVersion;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

final class BridgeConfig {
    private static final String DEFAULT_CONFIG = """
            # DimensionBridge - Velocity
            # Server names must exactly match the names in velocity.toml.
            # MINECRAFT_1_20 covers the shared 1.20/1.20.1 network protocol.
            # Use * for every protocol or every source server.

            initial-servers=lobby
            deny-unlisted-targets=false
            cooldown-ms=2500
            authorization-window-ms=5000
            bypass-permission=dimensionbridge.bypass

            destinations=lobby,conquest,world1201b,world262

            destination.lobby.display-name=Lobby
            destination.lobby.protected=false
            destination.lobby.allowed-sources=*
            destination.lobby.allowed-protocols=*

            destination.conquest.display-name=Conquest Reforged
            destination.conquest.protected=true
            destination.conquest.allowed-sources=lobby
            destination.conquest.allowed-protocols=MINECRAFT_1_20

            destination.world1201b.display-name=Zweite 1.20.1-Welt
            destination.world1201b.protected=true
            destination.world1201b.allowed-sources=lobby
            destination.world1201b.allowed-protocols=MINECRAFT_1_20

            destination.world262.display-name=26.2-Welt
            destination.world262.protected=true
            destination.world262.allowed-sources=lobby
            destination.world262.allowed-protocols=MINECRAFT_26_2
            """;

    private final Set<String> initialServers;
    private final boolean denyUnlistedTargets;
    private final long cooldownMillis;
    private final long authorizationWindowMillis;
    private final String bypassPermission;
    private final Map<String, DestinationRule> destinations;

    private BridgeConfig(
            Set<String> initialServers,
            boolean denyUnlistedTargets,
            long cooldownMillis,
            long authorizationWindowMillis,
            String bypassPermission,
            Map<String, DestinationRule> destinations
    ) {
        this.initialServers = Set.copyOf(initialServers);
        this.denyUnlistedTargets = denyUnlistedTargets;
        this.cooldownMillis = cooldownMillis;
        this.authorizationWindowMillis = authorizationWindowMillis;
        this.bypassPermission = bypassPermission;
        this.destinations = Map.copyOf(destinations);
    }

    static BridgeConfig load(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        if (Files.notExists(file)) {
            Files.writeString(file, DEFAULT_CONFIG, StandardCharsets.UTF_8);
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        Set<String> initialServers = parseCsv(properties.getProperty("initial-servers", "lobby"), true);
        boolean denyUnlistedTargets = Boolean.parseBoolean(properties.getProperty("deny-unlisted-targets", "false"));
        long cooldownMillis = parseLong(properties, "cooldown-ms", 2500L, 0L, 60_000L);
        long authorizationWindowMillis = parseLong(properties, "authorization-window-ms", 5000L, 500L, 60_000L);
        String bypassPermission = properties.getProperty("bypass-permission", "dimensionbridge.bypass").trim();

        Set<String> destinationNames = parseCsv(properties.getProperty("destinations", "lobby,conquest"), true);
        Map<String, DestinationRule> destinations = new LinkedHashMap<>();

        for (String name : destinationNames) {
            String prefix = "destination." + name + ".";
            String displayName = properties.getProperty(prefix + "display-name", name).trim();
            boolean protectedTarget = Boolean.parseBoolean(properties.getProperty(prefix + "protected", "true"));
            Set<String> allowedSources = parseCsv(properties.getProperty(prefix + "allowed-sources", "lobby"), true);
            Set<String> allowedProtocols = parseCsv(properties.getProperty(prefix + "allowed-protocols", "*"), false);
            validateProtocols(name, allowedProtocols);

            destinations.put(name, new DestinationRule(
                    name,
                    displayName.isBlank() ? name : displayName,
                    protectedTarget,
                    allowedSources,
                    allowedProtocols
            ));
        }

        if (destinations.isEmpty()) {
            throw new IllegalArgumentException("Mindestens ein Ziel muss in 'destinations' stehen.");
        }

        return new BridgeConfig(
                initialServers,
                denyUnlistedTargets,
                cooldownMillis,
                authorizationWindowMillis,
                bypassPermission,
                destinations
        );
    }

    private static void validateProtocols(String destination, Set<String> protocols) {
        for (String protocol : protocols) {
            if (protocol.equals("*")) {
                continue;
            }
            try {
                ProtocolVersion.valueOf(protocol);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Unbekanntes Velocity-Protokoll '" + protocol + "' bei Ziel '" + destination + "'.",
                        exception
                );
            }
        }
    }

    private static long parseLong(Properties properties, String key, long defaultValue, long minimum, long maximum) {
        String raw = properties.getProperty(key, Long.toString(defaultValue)).trim();
        try {
            long value = Long.parseLong(raw);
            if (value < minimum || value > maximum) {
                throw new IllegalArgumentException(key + " muss zwischen " + minimum + " und " + maximum + " liegen.");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Ungültiger Zahlenwert für " + key + ": " + raw, exception);
        }
    }

    private static Set<String> parseCsv(String raw, boolean lowercase) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptySet();
        }

        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> lowercase ? value.toLowerCase(Locale.ROOT) : value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    Set<String> initialServers() {
        return initialServers;
    }

    boolean denyUnlistedTargets() {
        return denyUnlistedTargets;
    }

    long cooldownMillis() {
        return cooldownMillis;
    }

    long authorizationWindowMillis() {
        return authorizationWindowMillis;
    }

    String bypassPermission() {
        return bypassPermission;
    }

    Map<String, DestinationRule> destinations() {
        return destinations;
    }

    DestinationRule destination(String serverName) {
        if (serverName == null) {
            return null;
        }
        return destinations.get(serverName.toLowerCase(Locale.ROOT));
    }

    record DestinationRule(
            String serverName,
            String displayName,
            boolean protectedTarget,
            Set<String> allowedSources,
            Set<String> allowedProtocols
    ) {
        DestinationRule {
            Objects.requireNonNull(serverName, "serverName");
            Objects.requireNonNull(displayName, "displayName");
            allowedSources = Set.copyOf(allowedSources);
            allowedProtocols = Set.copyOf(allowedProtocols);
        }

        boolean allowsSource(String sourceServer) {
            if (allowedSources.contains("*")) {
                return true;
            }
            return sourceServer != null && allowedSources.contains(sourceServer.toLowerCase(Locale.ROOT));
        }

        boolean allowsProtocol(ProtocolVersion protocolVersion) {
            return allowedProtocols.contains("*") || allowedProtocols.contains(protocolVersion.name());
        }
    }
}
