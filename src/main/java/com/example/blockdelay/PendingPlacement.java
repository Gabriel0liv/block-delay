package com.example.blockdelay;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class PendingPlacement {
    private final UUID playerId;
    private final ResourceKey<Level> dimension;
    private final BlockPos clickedPos;
    private final Direction clickedFace;
    private final Vec3 hitLocation;
    private final boolean insideBlock;
    private final InteractionHand hand;
    private final ItemStack stackSnapshot;
    private final ResourceLocation blockId;
    private final int requiredTicks;
    private final double maxDistance;
    private final long createdGameTime;
    private int ticksElapsed;

    PendingPlacement(
            UUID playerId,
            ResourceKey<Level> dimension,
            BlockPos clickedPos,
            Direction clickedFace,
            Vec3 hitLocation,
            boolean insideBlock,
            InteractionHand hand,
            ItemStack stackSnapshot,
            ResourceLocation blockId,
            int requiredTicks,
            double maxDistance,
            long createdGameTime) {
        this.playerId = playerId;
        this.dimension = dimension;
        this.clickedPos = clickedPos.immutable();
        this.clickedFace = clickedFace;
        this.hitLocation = hitLocation;
        this.insideBlock = insideBlock;
        this.hand = hand;
        this.stackSnapshot = stackSnapshot.copy();
        this.blockId = blockId;
        this.requiredTicks = requiredTicks;
        this.maxDistance = maxDistance;
        this.createdGameTime = createdGameTime;
    }

    UUID playerId() {
        return playerId;
    }

    ResourceKey<Level> dimension() {
        return dimension;
    }

    BlockPos clickedPos() {
        return clickedPos;
    }

    Direction clickedFace() {
        return clickedFace;
    }

    Vec3 hitLocation() {
        return hitLocation;
    }

    boolean insideBlock() {
        return insideBlock;
    }

    InteractionHand hand() {
        return hand;
    }

    ItemStack stackSnapshot() {
        return stackSnapshot;
    }

    ResourceLocation blockId() {
        return blockId;
    }

    int requiredTicks() {
        return requiredTicks;
    }

    double maxDistance() {
        return maxDistance;
    }

    long createdGameTime() {
        return createdGameTime;
    }

    int ticksElapsed() {
        return ticksElapsed;
    }

    int advance() {
        this.ticksElapsed += 1;
        return this.ticksElapsed;
    }

    boolean isReady() {
        return this.ticksElapsed >= this.requiredTicks;
    }

    BlockHitResult createHitResult() {
        return new BlockHitResult(this.hitLocation, this.clickedFace, this.clickedPos, this.insideBlock);
    }
}
