package dev.dimensionbridge.fabric262;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TransferRequestPayload(String destination) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("dimensionbridge", "transfer");
    public static final Type<TransferRequestPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, TransferRequestPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            TransferRequestPayload::destination,
            TransferRequestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
