package com.example.blockdelay;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

final class DelayedPlacementHandler {
    private static final int HELD_HEARTBEAT_TIMEOUT_TICKS = 10;

    private DelayedPlacementHandler() {
    }

    static void onUseItemOnBlock(UseItemOnBlockEvent event) {
        if (event.getSide().isClient()) {
            return;
        }

        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK) {
            return;
        }

        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        if (PendingPlacementManager.isBypassing(player.getUUID())) {
            debug("Bypassing delayed placement hook for {}", player.getGameProfile().getName());
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        if (!shouldDelay(player)) {
            return;
        }

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
        if (isBlacklisted(blockItem.getBlock(), blockId)) {
            debug("Skipping delay for blacklisted block {} used by {}", blockId, player.getGameProfile().getName());
            return;
        }

        UseOnContext context = event.getUseOnContext();
        BlockHitResult hitResult = new BlockHitResult(
                context.getClickLocation(),
                context.getClickedFace(),
                context.getClickedPos(),
                context.isInside());
        PendingPlacement pendingPlacement = new PendingPlacement(
                player.getUUID(),
                player.level().dimension(),
                hitResult.getBlockPos(),
                hitResult.getDirection(),
                hitResult.getLocation(),
                hitResult.isInside(),
                event.getHand(),
                stack.copy(),
                blockId,
                BlockDelayConfig.PLACEMENT_DELAY_TICKS.getAsInt(),
                BlockDelayConfig.MAX_DISTANCE_FROM_TARGET.get(),
                player.level().getGameTime());

        PendingPlacement replaced = PendingPlacementManager.put(pendingPlacement);
        if (replaced != null) {
            debug("Replaced pending placement for {}", player.getGameProfile().getName());
        }

        debug(
                "Queued delayed placement: player={}, block={}, hand={}, pos={}, delayTicks={}",
                player.getGameProfile().getName(),
                blockId,
                event.getHand(),
                hitResult.getBlockPos(),
                pendingPlacement.requiredTicks());
        event.cancelWithResult(ItemInteractionResult.SUCCESS);
    }

    static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PendingPlacement pendingPlacement = PendingPlacementManager.get(player.getUUID());
        if (pendingPlacement == null) {
            return;
        }

        String failureReason = validate(player, pendingPlacement);
        if (failureReason != null) {
            cancelPending(player, pendingPlacement, failureReason);
            return;
        }

        int ticksElapsed = pendingPlacement.advance();
        updateActionBar(player, pendingPlacement);
        if (BlockDelayConfig.SHOW_PARTICLES.getAsBoolean() && player.level() instanceof ServerLevel serverLevel && ticksElapsed % 4 == 0) {
            Vec3 hitLocation = pendingPlacement.hitLocation();
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.END_ROD,
                    hitLocation.x,
                    hitLocation.y,
                    hitLocation.z,
                    2,
                    0.05D,
                    0.05D,
                    0.05D,
                    0.0D);
        }

        debug(
                "Placement progress: player={}, block={}, progress={}/{}",
                player.getGameProfile().getName(),
                pendingPlacement.blockId(),
                ticksElapsed,
                pendingPlacement.requiredTicks());

        if (!pendingPlacement.isReady()) {
            return;
        }

        attemptFinalPlacement(player, pendingPlacement);
    }

    static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PendingPlacement removed = PendingPlacementManager.remove(event.getEntity().getUUID());
        PendingPlacementManager.clearUseHeld(event.getEntity().getUUID());
        if (removed != null) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                clearActionBar(serverPlayer);
            }
            debug("Cancelled pending placement on logout for {}", event.getEntity().getGameProfile().getName());
        }
    }

    static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        PendingPlacement removed = PendingPlacementManager.remove(event.getEntity().getUUID());
        if (removed != null) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                clearActionBar(serverPlayer);
            }
            debug("Cancelled pending placement on dimension change for {}", event.getEntity().getGameProfile().getName());
        }
    }

    static void onUseKeyHeldUpdate(ServerPlayer player, boolean held) {
        PendingPlacementManager.updateUseHeld(player.getUUID(), held, player.level().getGameTime());
        debug("Updated held-input state: player={}, held={}", player.getGameProfile().getName(), held);

        if (!held) {
            PendingPlacement pendingPlacement = PendingPlacementManager.get(player.getUUID());
            if (pendingPlacement != null) {
                cancelPending(player, pendingPlacement, "use key released");
            }
        }
    }

    private static boolean shouldDelay(ServerPlayer player) {
        if (!BlockDelayConfig.ONLY_SURVIVAL.getAsBoolean()) {
            return true;
        }

        return player.gameMode.isSurvival();
    }

    private static boolean isBlacklisted(Block block, ResourceLocation blockId) {
        List<? extends String> blockIds = BlockDelayConfig.BLACKLISTED_BLOCKS.get();
        if (blockIds.contains(blockId.toString())) {
            return true;
        }

        List<? extends String> modIds = BlockDelayConfig.BLACKLISTED_MODS.get();
        if (modIds.contains(blockId.getNamespace())) {
            return true;
        }

        for (String rawTagId : BlockDelayConfig.BLACKLISTED_BLOCK_TAGS.get()) {
            ResourceLocation tagId = ResourceLocation.tryParse(rawTagId);
            if (tagId == null) {
                continue;
            }

            TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, tagId);
            if (block.defaultBlockState().is(tagKey)) {
                return true;
            }
        }

        return false;
    }

    private static String validate(ServerPlayer player, PendingPlacement pendingPlacement) {
        if (!player.isAlive()) {
            return "player died";
        }

        if (!player.level().dimension().equals(pendingPlacement.dimension())) {
            return "dimension changed";
        }

        if (!shouldDelay(player)) {
            return "player is no longer in survival";
        }

        ItemStack currentStack = player.getItemInHand(pendingPlacement.hand());
        if (currentStack.isEmpty()) {
            return "hand is now empty";
        }

        if (!ItemStack.isSameItemSameComponents(currentStack, pendingPlacement.stackSnapshot())) {
            return "held item changed";
        }

        if (player.getEyePosition().distanceToSqr(pendingPlacement.hitLocation()) > pendingPlacement.maxDistance() * pendingPlacement.maxDistance()) {
            return "player moved too far away";
        }

        HitResult hitResult = player.pick(Math.max(player.blockInteractionRange(), pendingPlacement.maxDistance()), 0.0F, false);
        if (!(hitResult instanceof BlockHitResult blockHitResult) || blockHitResult.getType() != HitResult.Type.BLOCK) {
            return "player is no longer aiming at a block";
        }

        BlockPos currentPos = blockHitResult.getBlockPos();
        Direction currentFace = blockHitResult.getDirection();
        if (!currentPos.equals(pendingPlacement.clickedPos()) || currentFace != pendingPlacement.clickedFace()) {
            return "player looked away from the target";
        }

        Block currentBlock = ((BlockItem) currentStack.getItem()).getBlock();
        ResourceLocation currentBlockId = BuiltInRegistries.BLOCK.getKey(currentBlock);
        if (isBlacklisted(currentBlock, currentBlockId)) {
            return "block became blacklisted";
        }

        if (BlockDelayConfig.REQUIRE_USE_KEY_HELD.getAsBoolean()) {
            long ageTicks = player.level().getGameTime() - pendingPlacement.createdGameTime();
            if (ageTicks > BlockDelayConfig.HOLD_INPUT_GRACE_TICKS.getAsInt() && !isUseHeldValid(player)) {
                return "use key is no longer held";
            }
        }

        return null;
    }

    private static void attemptFinalPlacement(ServerPlayer player, PendingPlacement pendingPlacement) {
        ItemStack currentStack = player.getItemInHand(pendingPlacement.hand());
        UseOnContext useOnContext = new UseOnContext(player.level(), player, pendingPlacement.hand(), currentStack, pendingPlacement.createHitResult());
        InteractionResult result;

        PendingPlacementManager.beginBypass(player.getUUID());
        try {
            result = currentStack.useOn(useOnContext);
        } finally {
            PendingPlacementManager.endBypass(player.getUUID());
        }

        PendingPlacementManager.remove(player.getUUID());
        clearActionBar(player);
        debug(
                "Final placement attempt: player={}, block={}, result={}, remainingStack={}",
                player.getGameProfile().getName(),
                pendingPlacement.blockId(),
                result,
                currentStack.getCount());
    }

    private static void cancelPending(ServerPlayer player, PendingPlacement pendingPlacement, String reason) {
        PendingPlacementManager.remove(player.getUUID());
        clearActionBar(player);
        debug(
                "Cancelled pending placement: player={}, block={}, reason={}",
                player.getGameProfile().getName(),
                pendingPlacement.blockId(),
                reason);
    }

    private static boolean isUseHeldValid(ServerPlayer player) {
        PendingPlacementManager.HeldInputState heldInputState = PendingPlacementManager.getUseHeldState(player.getUUID());
        if (heldInputState == null || !heldInputState.held()) {
            return false;
        }

        long ageTicks = player.level().getGameTime() - heldInputState.lastUpdateGameTime();
        return ageTicks <= HELD_HEARTBEAT_TIMEOUT_TICKS;
    }

    private static void updateActionBar(ServerPlayer player, PendingPlacement pendingPlacement) {
        if (!BlockDelayConfig.SHOW_PROGRESS_ACTION_BAR.getAsBoolean()) {
            return;
        }

        int requiredTicks = pendingPlacement.requiredTicks();
        int elapsedTicks = pendingPlacement.ticksElapsed();
        int progressBarLength = BlockDelayConfig.PROGRESS_BAR_LENGTH.getAsInt();
        int filledSegments = Math.max(0, Math.min(progressBarLength, (int) Math.floor((elapsedTicks / (double) requiredTicks) * progressBarLength)));

        String filled = "|".repeat(filledSegments);
        String empty = ".".repeat(progressBarLength - filledSegments);
        int percent = Math.max(0, Math.min(100, (int) Math.floor((elapsedTicks / (double) requiredTicks) * 100.0D)));
        player.displayClientMessage(Component.literal("Placing [" + filled + empty + "] " + percent + "%"), true);
    }

    private static void clearActionBar(ServerPlayer player) {
        if (BlockDelayConfig.SHOW_PROGRESS_ACTION_BAR.getAsBoolean() && BlockDelayConfig.CLEAR_ACTION_BAR_ON_CANCEL.getAsBoolean()) {
            player.displayClientMessage(Component.empty(), true);
        }
    }

    private static void debug(String message, Object... args) {
        if (BlockDelayConfig.ENABLE_DEBUG_LOGGING.getAsBoolean()) {
            BlockDelayMod.LOGGER.info("[blockdelay] " + message, args);
        }
    }
}
