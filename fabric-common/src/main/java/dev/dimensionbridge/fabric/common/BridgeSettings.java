package dev.dimensionbridge.fabric.common;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class BridgeSettings {
    private static final Pattern SERVER_NAME_PATTERN = Pattern.compile("[a-z0-9_-]{1,64}");
    private static final String DEFAULT_CONFIG = """
            # Lokale Positivliste. Velocity prüft das Ziel zusätzlich erneut.
            # * erlaubt jeden syntaktisch gültigen, auf Velocity konfigurierten Zielnamen.
            allowed-destinations=lobby,hauptwelt,hardcore,vanilla
            """;

    private volatile Set<String> allowedDestinations = Set.of();

    public void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("dimensionbridge-fabric.properties");
        try {
            Files.createDirectories(file.getParent());
            if (Files.notExists(file)) {
                Files.writeString(file, DEFAULT_CONFIG, StandardCharsets.UTF_8);
            }

            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }

            Set<String> parsed = Arrays.stream(properties.getProperty("allowed-destinations", "*").split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .map(value -> value.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            allowedDestinations = Set.copyOf(parsed);
        } catch (IOException exception) {
            throw new IllegalStateException("DimensionBridge-Konfiguration konnte nicht geladen werden: " + file, exception);
        }
    }

    public boolean allows(String destination) {
        return allowedDestinations.contains("*") || allowedDestinations.contains(destination);
    }

    public static String normalizeDestination(String raw) {
        String destination = raw.trim().toLowerCase(Locale.ROOT);
        if (!SERVER_NAME_PATTERN.matcher(destination).matches()) {
            throw new IllegalArgumentException("Ungültiger Zielserver: " + raw);
        }
        return destination;
    }
}
