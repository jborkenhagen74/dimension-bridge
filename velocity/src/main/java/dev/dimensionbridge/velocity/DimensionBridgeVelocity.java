package dev.dimensionbridge.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@Plugin(
        id = "dimensionbridge",
        name = "DimensionBridge",
        version = "1.2.1",
        description = "Sichere, serverseitige Dimensionstransfers zwischen Fabric-Backends und Velocity.",
        authors = {"Jan Borkenhagen"}
)
public final class DimensionBridgeVelocity {
    private static final MinecraftChannelIdentifier TRANSFER_CHANNEL =
            MinecraftChannelIdentifier.from("dimensionbridge:transfer");
    private static final Pattern SERVER_NAME_PATTERN = Pattern.compile("[a-z0-9_-]{1,64}");
    private static final Component PREFIX = Component.text("[Dimensionsportal] ", NamedTextColor.DARK_AQUA);

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path configFile;
    private final AtomicReference<BridgeConfig> config = new AtomicReference<>();
    private final Map<UUID, PendingAuthorization> pendingAuthorizations = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldownUntil = new ConcurrentHashMap<>();

    @Inject
    public DimensionBridgeVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.configFile = dataDirectory.resolve("dimensionbridge.properties");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        reloadConfig();
        proxy.getChannelRegistrar().register(TRANSFER_CHANNEL);

        CommandMeta commandMeta = proxy.getCommandManager()
                .metaBuilder("dimensionbridge")
                .aliases("db")
                .plugin(this)
                .build();
        proxy.getCommandManager().register(commandMeta, new AdminCommand());

        logger.info("DimensionBridge wurde geladen. Kanal: {}", TRANSFER_CHANNEL.getId());
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!TRANSFER_CHANNEL.equals(event.getIdentifier())) {
            return;
        }

        // Nie zum Client oder zu einem anderen Backend weiterleiten.
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!(event.getSource() instanceof ServerConnection backend)) {
            logger.warn("Verworfene DimensionBridge-Nachricht, deren Quelle kein Backend war.");
            return;
        }

        Player player = backend.getPlayer();
        String sourceServer = backend.getServerInfo().getName().toLowerCase(Locale.ROOT);

        Optional<ServerConnection> currentConnection = player.getCurrentServer();
        if (currentConnection.isEmpty()
                || !currentConnection.get().getServerInfo().getName().equalsIgnoreCase(sourceServer)) {
            logger.warn("Verworfene veraltete Bridge-Nachricht für {} von {}.", player.getUsername(), sourceServer);
            return;
        }

        final String destination;
        try {
            destination = normalizeDestination(NetworkCodec.decodeDestination(event.getData()));
        } catch (IllegalArgumentException exception) {
            logger.warn("Ungültige Bridge-Nachricht von Backend {}: {}", sourceServer, exception.getMessage());
            return;
        }

        requestTransfer(player, sourceServer, destination);
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        BridgeConfig currentConfig = config.get();
        if (currentConfig == null) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            return;
        }

        Player player = event.getPlayer();
        String targetName = event.getOriginalServer().getServerInfo().getName().toLowerCase(Locale.ROOT);
        String sourceName = event.getPreviousServer() == null
                ? null
                : event.getPreviousServer().getServerInfo().getName().toLowerCase(Locale.ROOT);

        if (hasBypass(player, currentConfig)) {
            return;
        }

        BridgeConfig.DestinationRule rule = currentConfig.destination(targetName);

        // Der erste Login darf nur auf ausdrücklich konfigurierte Einstiegsserver gehen.
        if (event.getPreviousServer() == null) {
            if (currentConfig.initialServers().contains(targetName)
                    && (rule == null || rule.allowsProtocol(player.getProtocolVersion()))) {
                return;
            }

            if (rule != null && !rule.protectedTarget() && rule.allowsProtocol(player.getProtocolVersion())) {
                return;
            }

            deny(event, player, "Dieser Einstiegsserver ist nicht freigegeben.");
            return;
        }

        if (rule == null) {
            if (currentConfig.denyUnlistedTargets()) {
                deny(event, player, "Dieses Ziel ist nicht in DimensionBridge konfiguriert.");
            }
            return;
        }

        if (!rule.allowsProtocol(player.getProtocolVersion())) {
            deny(event, player, versionDeniedMessage(player.getProtocolVersion(), rule));
            return;
        }

        if (!rule.protectedTarget()) {
            return;
        }

        PendingAuthorization authorization = pendingAuthorizations.remove(player.getUniqueId());
        long now = System.currentTimeMillis();
        boolean valid = authorization != null
                && authorization.expiresAtMillis() >= now
                && authorization.targetServer().equals(targetName)
                && authorization.sourceServer().equals(sourceName)
                && rule.allowsSource(sourceName);

        if (!valid) {
            deny(event, player, "Dieser Server ist ausschließlich über eine freigegebene Dimensionsportal erreichbar.");
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        pendingAuthorizations.remove(playerId);
        cooldownUntil.remove(playerId);
    }

    private void requestTransfer(Player player, String sourceServer, String destination) {
        BridgeConfig currentConfig = config.get();
        if (currentConfig == null) {
            player.sendMessage(PREFIX.append(Component.text("Die Bridge-Konfiguration ist nicht verfügbar.", NamedTextColor.RED)));
            return;
        }

        BridgeConfig.DestinationRule rule = currentConfig.destination(destination);
        if (rule == null) {
            player.sendMessage(PREFIX.append(Component.text("Unbekannte Dimensionskennung bzw. unbekanntes Ziel.", NamedTextColor.RED)));
            logger.warn("Backend {} forderte das nicht konfigurierte Ziel '{}' für {} an.",
                    sourceServer, destination, player.getUsername());
            return;
        }

        if (!rule.allowsSource(sourceServer)) {
            player.sendMessage(PREFIX.append(Component.text("Dieses Dimensionsportal darf dieses Ziel nicht anwählen.", NamedTextColor.RED)));
            logger.warn("Nicht erlaubte Route {} -> {} für {}.", sourceServer, destination, player.getUsername());
            return;
        }

        if (!rule.allowsProtocol(player.getProtocolVersion())) {
            player.sendMessage(PREFIX.append(Component.text(
                    versionDeniedMessage(player.getProtocolVersion(), rule),
                    NamedTextColor.RED
            )));
            return;
        }

        if (sourceServer.equals(destination)) {
            player.sendMessage(PREFIX.append(Component.text("Du bist bereits mit diesem Server verbunden.", NamedTextColor.YELLOW)));
            return;
        }

        long now = System.currentTimeMillis();
        long blockedUntil = cooldownUntil.getOrDefault(player.getUniqueId(), 0L);
        if (blockedUntil > now) {
            long remainingMillis = blockedUntil - now;
            player.sendMessage(PREFIX.append(Component.text(
                    "Die Leitung ist noch belegt (" + Math.max(1, (remainingMillis + 999) / 1000) + " s).",
                    NamedTextColor.YELLOW
            )));
            return;
        }

        Optional<RegisteredServer> target = proxy.getServer(destination);
        if (target.isEmpty()) {
            player.sendMessage(PREFIX.append(Component.text("Der Zielserver ist im Proxy nicht registriert.", NamedTextColor.RED)));
            logger.error("Konfiguriertes Ziel '{}' existiert nicht in velocity.toml.", destination);
            return;
        }

        PendingAuthorization authorization = new PendingAuthorization(
                sourceServer,
                destination,
                now + currentConfig.authorizationWindowMillis()
        );
        pendingAuthorizations.put(player.getUniqueId(), authorization);
        cooldownUntil.put(player.getUniqueId(), now + currentConfig.cooldownMillis());

        player.sendMessage(PREFIX.append(Component.text(
                "Verbindung zu " + rule.displayName() + " wird hergestellt …",
                NamedTextColor.AQUA
        )));

        player.createConnectionRequest(target.get()).connect().whenComplete((result, throwable) -> {
            if (throwable != null) {
                pendingAuthorizations.remove(player.getUniqueId(), authorization);
                logger.error("Transfer von {} zu {} ist fehlgeschlagen.", player.getUsername(), destination, throwable);
                player.sendMessage(PREFIX.append(Component.text("Die Verbindung ist fehlgeschlagen.", NamedTextColor.RED)));
                return;
            }

            if (!result.isSuccessful()) {
                pendingAuthorizations.remove(player.getUniqueId(), authorization);
                Component message = PREFIX.append(Component.text(
                        "Die Verbindung wurde nicht hergestellt (" + result.getStatus() + ").",
                        NamedTextColor.RED
                ));
                if (result.getReasonComponent().isPresent()) {
                    message = message.append(Component.space()).append(result.getReasonComponent().get());
                }
                player.sendMessage(message);
            }
        });
    }

    private void deny(ServerPreConnectEvent event, Player player, String message) {
        event.setResult(ServerPreConnectEvent.ServerResult.denied());
        pendingAuthorizations.remove(player.getUniqueId());
        player.sendMessage(PREFIX.append(Component.text(message, NamedTextColor.RED)));
    }

    private boolean hasBypass(Player player, BridgeConfig currentConfig) {
        String permission = currentConfig.bypassPermission();
        return permission != null && !permission.isBlank() && player.hasPermission(permission);
    }

    private static String normalizeDestination(String destination) {
        String normalized = destination.trim().toLowerCase(Locale.ROOT);
        if (!SERVER_NAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Ungültiger Zielserver: " + destination);
        }
        return normalized;
    }

    private static String versionDeniedMessage(
            ProtocolVersion clientProtocol,
            BridgeConfig.DestinationRule rule
    ) {
        String allowed = String.join(", ", rule.allowedProtocols());
        return "Falsche Minecraft-Version. Dein Protokoll: "
                + clientProtocol.name()
                + "; für "
                + rule.displayName()
                + " erlaubt: "
                + allowed;
    }

    private synchronized boolean reloadConfig() {
        try {
            BridgeConfig loaded = BridgeConfig.load(configFile);
            config.set(loaded);
            logger.info("DimensionBridge-Konfiguration geladen: {} Ziele, Einstiegsserver: {}",
                    loaded.destinations().size(), loaded.initialServers());
            return true;
        } catch (IOException | IllegalArgumentException exception) {
            logger.error("DimensionBridge-Konfiguration konnte nicht geladen werden: {}", configFile, exception);
            if (config.get() == null) {
                throw new IllegalStateException("DimensionBridge kann ohne gültige Erstkonfiguration nicht starten.", exception);
            }
            return false;
        }
    }

    private record PendingAuthorization(String sourceServer, String targetServer, long expiresAtMillis) {
    }

    private final class AdminCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] arguments = invocation.arguments();

            if (arguments.length == 0) {
                sendHelp(source);
                return;
            }

            switch (arguments[0].toLowerCase(Locale.ROOT)) {
                case "reload" -> {
                    try {
                        if (reloadConfig()) {
                            source.sendMessage(PREFIX.append(Component.text(
                                    "Konfiguration neu geladen.",
                                    NamedTextColor.GREEN
                            )));
                        } else {
                            source.sendMessage(PREFIX.append(Component.text(
                                    "Neuladen fehlgeschlagen; die bisherige Konfiguration bleibt aktiv.",
                                    NamedTextColor.RED
                            )));
                        }
                    } catch (RuntimeException exception) {
                        source.sendMessage(PREFIX.append(Component.text(
                                "Neuladen fehlgeschlagen; Details stehen in der Proxy-Konsole.",
                                NamedTextColor.RED
                        )));
                    }
                }
                case "protocol" -> showProtocol(source, arguments);
                case "info" -> showInfo(source);
                default -> sendHelp(source);
            }
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return !(invocation.source() instanceof Player)
                    || invocation.source().hasPermission("dimensionbridge.admin");
        }

        @Override
        public List<String> suggest(Invocation invocation) {
            String[] arguments = invocation.arguments();
            if (arguments.length <= 1) {
                return filterPrefix(List.of("reload", "protocol", "info"), arguments.length == 0 ? "" : arguments[0]);
            }
            if (arguments.length == 2 && arguments[0].equalsIgnoreCase("protocol")) {
                return filterPrefix(
                        proxy.getAllPlayers().stream().map(Player::getUsername).sorted().toList(),
                        arguments[1]
                );
            }
            return List.of();
        }

        private void showProtocol(CommandSource source, String[] arguments) {
            Player target;
            if (arguments.length >= 2) {
                target = proxy.getPlayer(arguments[1]).orElse(null);
                if (target == null) {
                    source.sendMessage(PREFIX.append(Component.text("Spieler nicht gefunden.", NamedTextColor.RED)));
                    return;
                }
            } else if (source instanceof Player player) {
                target = player;
            } else {
                source.sendMessage(PREFIX.append(Component.text("Bitte einen Spielernamen angeben.", NamedTextColor.YELLOW)));
                return;
            }

            ProtocolVersion protocol = target.getProtocolVersion();
            source.sendMessage(PREFIX.append(Component.text(
                    target.getUsername() + ": " + protocol.name() + " (ID " + protocol.getProtocol() + ", "
                            + String.join("/", protocol.getVersionsSupportedBy()) + ")",
                    NamedTextColor.AQUA
            )));
        }

        private void showInfo(CommandSource source) {
            BridgeConfig currentConfig = config.get();
            source.sendMessage(PREFIX.append(Component.text(
                    "Konfiguration: " + configFile + "; Ziele: " + currentConfig.destinations().keySet(),
                    NamedTextColor.GRAY
            )));
        }

        private void sendHelp(CommandSource source) {
            source.sendMessage(PREFIX.append(Component.text(
                    "/dimensionbridge reload | protocol [Spieler] | info",
                    NamedTextColor.GRAY
            )));
        }

        private List<String> filterPrefix(Collection<String> values, String prefix) {
            String normalized = prefix.toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            for (String value : values) {
                if (value.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                    result.add(value);
                }
            }
            return result;
        }
    }
}
