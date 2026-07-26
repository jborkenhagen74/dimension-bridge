package dev.dimensionbridge.fabric;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.dimensionbridge.fabric.common.BridgeSettings;
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

import java.util.Collection;

public final class DimensionBridgeFabric implements ModInitializer {
    public static final String MOD_ID = "dimensionbridge";
    private static final Logger LOGGER = LoggerFactory.getLogger("DimensionBridge/Fabric");
    private final BridgeSettings settings = new BridgeSettings();

    @Override
    public void onInitialize() {
        settings.load();
        PayloadTypeRegistry.playS2C().register(TransferRequestPayload.TYPE, TransferRequestPayload.CODEC);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("dimensionbridge")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                        .then(Commands.literal("transfer")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("destination", StringArgumentType.word())
                                                .executes(this::executeTransfer))))));

        String version = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unbekannt");
        LOGGER.info("DimensionBridge-Backend {} geladen. Kanal: {}", version, TransferRequestPayload.ID);
    }

    private int executeTransfer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        String destination = BridgeSettings.normalizeDestination(StringArgumentType.getString(context, "destination"));
        if (!settings.allows(destination)) {
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
}
