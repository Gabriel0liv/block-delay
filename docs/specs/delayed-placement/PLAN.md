# Delayed Placement Held-Input Plan

## Proposed Architecture

- Keep the existing server-side delayed placement flow and bypass guard.
- Register a common config spec through the mod container.
- Register a dedicated server-side event handler on the NeoForge event bus.
- Register NeoForge payload handlers on the mod event bus.
- Register a client-only tick listener that watches Minecraft's configured `options.keyUse` state.
- Maintain a `PendingPlacementManager` keyed by player UUID.
- Track server-side held-input state keyed by player UUID.
- Progress pending placements from a server tick event.
- When the delay completes, re-run the original item placement path with a temporary bypass guard so the mod does not cancel its own final placement attempt.
- Show placement progress in the action bar from the server while a placement is pending.

## Event Hooks To Use

- Primary interception hook: `UseItemOnBlockEvent` during `UsePhase.ITEM_AFTER_BLOCK`
- Tick progression hook: `PlayerTickEvent.Post` on the logical server
- Cleanup hooks: `PlayerEvent.PlayerLoggedOutEvent` and `PlayerEvent.PlayerChangedDimensionEvent`
- Client input hook: `ClientTickEvent.Post` on the physical client
- Network registration hook: `RegisterPayloadHandlersEvent` on the mod event bus

Implementation notes:

- `ITEM_AFTER_BLOCK` keeps normal block interaction routing intact and only delays actual item placement.
- The first placement event can arrive before the held-state packet, so the server must allow a short grace period.
- Client-only input code must only be registered on the physical client.
- A `BlockEvent.EntityPlaceEvent` fallback remains out of scope for this slice.

## Networking Architecture

- Payload type: `UseKeyHeldPayload(boolean held)`
- Direction: client to server
- Registration API: `RegisterPayloadHandlersEvent` with `PayloadRegistrar.playToServer(...)`
- Sending API: `PacketDistributor.sendToServer(...)`
- Codec strategy: `CustomPacketPayload` with `StreamCodec` and `ByteBufCodecs.BOOL`

Server packet handling:

- validate sender exists via `context.player()`
- update held-state for the sending player UUID
- stamp the server tick or level game time for freshness
- if `held=false`, cancel active pending placement for that player

Robustness:

- send on state transitions
- while held, send a low-frequency heartbeat so the server can recover from a missed release packet or stale state
- if heartbeat stops for too long, the server treats held-state as false

## Client Input Tracking Architecture

- Read `Minecraft.getInstance().options.keyUse.isDown()` on `ClientTickEvent.Post`
- Do not hardcode mouse buttons
- Compare current held state against the last sent state
- Send a payload on false -> true and true -> false transitions
- While held, send a periodic heartbeat only if needed for server freshness
- Skip sending when there is no local player or connection

## PendingPlacement Data Model

Fields:

- `UUID playerId`
- `ResourceKey<Level> dimension`
- `BlockPos clickedPos`
- `Direction clickedFace`
- `Vec3 hitLocation`
- `InteractionHand hand`
- `ItemStack stackSnapshot`
- `ResourceLocation blockId`
- `int ticksElapsed`
- `int requiredTicks`
- `double maxDistance`
- `long createdGameTime`

Notes:

- The original `BlockHitResult` can still be reconstructed from stored hit data when final placement is attempted.
- `createdGameTime` supports the initial held-input grace period.

## Server-Side Held-State Validation

- Store held-state per player UUID in `PendingPlacementManager`
- Track:
  - current held boolean
  - last server game-time when a held update arrived
- Consider held valid if:
  - current held flag is true, and
  - the last update is not older than a small timeout
- When a pending placement starts, allow a grace period of `holdInputGraceTicks` before requiring held-state to already be true
- After grace expires, not-held or stale-held cancels the pending placement

## Config Model

- `placementDelayTicks = 12`
- `onlySurvival = true`
- `maxDistanceFromTarget = 5.0`
- `enableDebugLogging = false`
- `showParticles = false`
- `requireUseKeyHeld = true`
- `holdInputGraceTicks = 3`
- `showProgressActionBar = true`
- `progressBarLength = 20`
- `clearActionBarOnCancel = true`
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
- if `requireUseKeyHeld` is enabled, the server still sees the use key as held or within the initial grace window

If any check fails, cancel the pending placement and log the reason when debug logging is enabled.

## Final Placement Strategy

- Reconstruct a `UseOnContext`-equivalent placement context from the player, hand, stack, and stored hit result.
- Set a temporary bypass marker for that player before invoking the item placement path.
- Call the held item's normal placement/use-on-block path so vanilla and modded `BlockItem` logic can run.
- Clear the bypass marker in a `finally` block.
- If placement succeeds, remove the pending entry.
- If placement fails, also remove the pending entry and log the failure; do not retry automatically.

## Progress Feedback Strategy

- Use `ServerPlayer.displayClientMessage(Component, true)` for action bar updates.
- Update progress while pending placement is active.
- Show a simple textual progress bar or tick counter.
- On cancel or completion, optionally clear the action bar with an empty action bar message.

## Risks And Fallbacks

- The selected NeoForge 1.21.1 hooks compile, but held-input timing still needs manual in-game testing.
- Held-state arrival may lag behind the initial placement event; the grace window must be long enough to avoid false cancels without letting click-and-release place blocks.
- Heartbeat timing that is too aggressive wastes packets; too relaxed risks stale held state lingering briefly.
- Some modded blocks may depend on highly dynamic client-side targeting details that are hard to reproduce later.
- Tag blacklist validation requires reliable tag lookup against the target block and current registry/tag API.
- Offhand interactions can be sensitive when mainhand items also react to the same click.
- Client animation may still play even when server placement is delayed; this is acceptable for the MVP if the server state remains correct.
- Custom HUD or cursor overlay is deferred to reduce client rendering risk in this slice.
