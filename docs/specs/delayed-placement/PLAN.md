# Delayed Placement MVP Plan

## Proposed Architecture

- Replace template metadata and sample content with a focused delayed-placement mod entrypoint.
- Register a common config spec through the mod container.
- Register a dedicated server-side event handler on the NeoForge event bus.
- Maintain a `PendingPlacementManager` keyed by player UUID.
- Progress pending placements from a server tick event.
- When the delay completes, re-run the original item placement path with a temporary bypass guard so the mod does not cancel its own final placement attempt.

## Event Hooks To Use

- Primary interception hook: `UseItemOnBlockEvent` during `UsePhase.ITEM_AFTER_BLOCK`
- Tick progression hook: `PlayerTickEvent.Post` on the logical server
- Cleanup hooks: `PlayerEvent.PlayerLoggedOutEvent` and `PlayerEvent.PlayerChangedDimensionEvent`

Implementation note:
- `ITEM_AFTER_BLOCK` is used instead of `RightClickBlock` so normal block interaction routing remains intact and only actual item placement is delayed.
- A `BlockEvent.EntityPlaceEvent` fallback is not used in the first slice because the primary hook compiled and covered the targeted `BlockItem` placement path cleanly.
- This means the MVP behaves as "click once, then keep aiming" rather than true continuous hold detection.

## PendingPlacement Data Model

Suggested fields:

- `UUID playerId`
- `ResourceKey<Level> dimension`
- `BlockPos clickedPos`
- `Direction clickedFace`
- `Vec3 hitLocation`
- `InteractionHand hand`
- `ResourceLocation itemId`
- `int itemCount`
- optional item component snapshot if needed
- `int ticksElapsed`
- `int requiredTicks`
- `double maxDistance`

Notes:

- For the MVP, item identity can likely be validated with item type plus count and hand slot state.
- The original `BlockHitResult` can be reconstructed from stored hit data when final placement is attempted.

## Config Model

- `placementDelayTicks = 12`
- `onlySurvival = true`
- `maxDistanceFromTarget = 5.0`
- `enableDebugLogging = true`
- `showParticles = false`
- `blacklistedBlocks = []`
- `blacklistedMods = []`
- `blacklistedBlockTags = []`

## Placement Validation Strategy

Each tick, validate:

- player still exists
- player is alive
- player is still in survival
- player is still in the same dimension
- player still has a compatible item in the same hand
- item stack is not empty
- player is still close enough to the target
- current ray trace still points at the same block face/target
- target block/item is not blacklisted
- the stored target still represents a legal placement attempt

If any check fails, cancel the pending placement and log the reason when debug logging is enabled.

## Final Placement Strategy

- Reconstruct a `UseOnContext`-equivalent placement context from the player, hand, stack, and stored hit result.
- Set a temporary bypass marker for that player before invoking the item placement path.
- Call the held item's normal placement/use-on-block path so vanilla and modded `BlockItem` logic can run.
- Clear the bypass marker in a `finally` block.
- If placement succeeds, remove the pending entry.
- If placement fails, also remove the pending entry and log the failure; do not retry automatically.

## Risks And Unknowns

- The exact NeoForge 1.21.1 interaction event name and cancellation semantics need verification.
- Some modded blocks may depend on highly dynamic client-side targeting details that are hard to reproduce later.
- Tag blacklist validation requires reliable tag lookup against the target block and current registry/tag API.
- Offhand interactions can be sensitive when mainhand items also react to the same click.
- Client animation may still play even when server placement is delayed; this is acceptable for the MVP if the server state remains correct.
- True "must hold right click" semantics are not implemented in this slice and likely require client-side input awareness or networking.
