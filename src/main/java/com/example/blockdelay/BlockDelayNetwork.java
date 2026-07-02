package com.example.blockdelay;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

final class BlockDelayNetwork {
    private BlockDelayNetwork() {
    }

    static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        event.registrar(BlockDelayMod.NETWORK_VERSION)
                .playToServer(UseKeyHeldPayload.TYPE, UseKeyHeldPayload.STREAM_CODEC, UseKeyHeldPayload::handleServer);
    }
}
