package com.example.blockdelay;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

record UseKeyHeldPayload(boolean held) implements CustomPacketPayload {
    static final CustomPacketPayload.Type<UseKeyHeldPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BlockDelayMod.MODID, "use_key_held"));
    static final StreamCodec<RegistryFriendlyByteBuf, UseKeyHeldPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            UseKeyHeldPayload::held,
            UseKeyHeldPayload::new);

    @Override
    public Type<UseKeyHeldPayload> type() {
        return TYPE;
    }

    static void handleServer(UseKeyHeldPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            DelayedPlacementHandler.onUseKeyHeldUpdate(serverPlayer, payload.held());
        }
    }
}
