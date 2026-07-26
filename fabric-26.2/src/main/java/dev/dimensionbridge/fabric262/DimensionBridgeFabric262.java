package dev.dimensionbridge.fabric262;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class DimensionBridgeFabric262 implements ModInitializer {
    public static final String MOD_ID = "dimensionbridge";
    private static final Logger LOGGER = LoggerFactory.getLogger("DimensionBridge/Fabric-26.2");
    private static final Pattern SERVER_NAME_PATTERN = Pattern.compile("[a-z0-9_-]{1,64}");
    private static final String DEFAULT_CONFIG = """
            # Lokale Positivliste. Velocity prüft das Ziel zusätzlich erneut.
            # * erlaubt jeden syntaktisch gültigen, auf Velocity konfigurierten Zielnamen.
            allowed-destinations=lobby,conquest,world1201b,world262
            """;

    private volatile Set<String> allowedDestinations = Set.of();

    @Override
    public void onInitialize() {
        loadConfig();

        // Die Payload wird ausschließlich serverseitig erzeugt und vom Velocity-Proxy abgefangen.
        PayloadTypeRegistry.clientboundPlay().register(TransferRequestPayload.TYPE, TransferRequestPayload.CODEC);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("dimensionbridge")
                        // COMMANDS_MODERATOR schließt Kommandoblöcke ein, aber keine normalen Spieler.
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                        .then(Commands.literal("transfer")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("destination", StringArgumentType.word())
                                                .executes(this::executeTransfer))))));

        LOGGER.info("DimensionBridge-Backend für Minecraft 26.2 geladen. Kanal: {}", TransferRequestPayload.ID);
    }

    private int executeTransfer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        String destination = normalizeDestination(StringArgumentType.getString(context, "destination"));

        if (!allowedDestinations.contains("*") && !allowedDestinations.contains(destination)) {
            context.getSource().sendFailure(Component.literal(
                    "DimensionBridge: Ziel '" + destination + "' ist lokal nicht freigegeben."
            ));
            return 0;
        }

        int sent = 0;
        for (ServerPlayer player : targets) {
            ServerPlayNetworking.send(player, new TransferRequestPayload(destination));
            sent++;
        }

        int finalSent = sent;
        context.getSource().sendSuccess(
                () -> Component.literal("DimensionBridge: " + finalSent + " Transferanfrage(n) nach '" + destination + "' gesendet."),
                false
        );
        return sent > 0 ? sent : Command.SINGLE_SUCCESS;
    }

    private void loadConfig() {
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

    private static String normalizeDestination(String raw) {
        String destination = raw.trim().toLowerCase(Locale.ROOT);
        if (!SERVER_NAME_PATTERN.matcher(destination).matches()) {
            throw new IllegalArgumentException("Ungültiger Zielserver: " + raw);
        }
        return destination;
    }
}
