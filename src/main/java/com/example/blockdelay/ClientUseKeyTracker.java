package com.example.blockdelay;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

final class ClientUseKeyTracker {
    private static final int HEARTBEAT_INTERVAL_TICKS = 5;

    private static boolean lastSentHeld;
    private static int ticksSinceLastHeartbeat;

    private ClientUseKeyTracker() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(ClientUseKeyTracker::onClientTickPost);
    }

    static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            lastSentHeld = false;
            ticksSinceLastHeartbeat = 0;
            return;
        }

        boolean held = minecraft.options.keyUse.isDown();
        boolean shouldSend = held != lastSentHeld;

        if (held) {
            ticksSinceLastHeartbeat += 1;
            if (ticksSinceLastHeartbeat >= HEARTBEAT_INTERVAL_TICKS) {
                shouldSend = true;
            }
        } else {
            ticksSinceLastHeartbeat = 0;
        }

        if (!shouldSend) {
            return;
        }

        PacketDistributor.sendToServer(new UseKeyHeldPayload(held));
        lastSentHeld = held;
        ticksSinceLastHeartbeat = 0;
    }
}
