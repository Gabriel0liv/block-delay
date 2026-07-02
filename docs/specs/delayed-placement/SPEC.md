# Delayed Placement Held-Input Spec

## Problem Statement

Instant block placement lets survival players rapidly box themselves in during combat-heavy gameplay. The server needs a server-authoritative delay for `BlockItem` placement so building remains possible, but panic placement becomes less effective. The existing MVP already delays placement server-side, but it still behaves as "click once, then keep aiming". This slice upgrades the MVP to require the player to keep holding Minecraft's configured use/place keybind.

## Current Repository Context

- Build system: Gradle with `net.neoforged.moddev`
- Minecraft target: `1.21.1`
- NeoForge target: `21.1.235`
- Java target: `21`
- Mod id: `blockdelay`
- Main package: `com.example.blockdelay`
- Server-side delayed-placement MVP is already implemented

## Scope

When a survival player attempts to place a block with a `BlockItem` in either hand:

1. Immediate placement is prevented.
2. A pending placement entry is stored on the server.
3. The client tracks whether the player is currently holding Minecraft's configured use/place keybind.
4. The player must remain valid, keep aiming at the same clicked block face, and keep the configured use/place key held for a configurable delay.
5. The server remains authoritative for all validation and final placement.
6. Releasing the configured use/place key before completion cancels the pending placement.
7. The player receives visible progress feedback through the action bar.
8. When the delay completes, the mod retries placement through the item's normal placement path.
9. If validation fails before completion, the pending placement is cancelled with no item consumption.

Important scope notes:

- This slice uses the configured Minecraft use/place keybind, not a hardcoded mouse button.
- The initial placement attempt still starts from the normal server-side use event.
- Because the held-state packet can arrive slightly after the initial placement event, the server allows a small configurable grace window before held-state becomes mandatory.
- Custom cursor or HUD overlay feedback is out of scope unless it turns out to be very small and safe.

## Functional Requirements

- `RF01`: Detect placement attempts for any item whose item type is `BlockItem`.
- `RF02`: Support `MAIN_HAND` and `OFF_HAND`.
- `RF03`: Apply only to survival players in the MVP.
- `RF04`: Delay must be configurable in ticks.
- `RF05`: Store pending placement state per player.
- `RF06`: Cancel pending placement if validation fails.
- `RF07`: Perform the real placement when progress reaches the configured delay.
- `RF08`: Prevent recursive cancellation when the mod performs the final placement.
- `RF09`: Support blacklist by block id.
- `RF10`: Support blacklist by namespace / mod id.
- `RF11`: Support blacklist by block tag id.
- `RF12`: Add simple debug logging for attempt, progress, cancel, and completion paths.
- `RF13`: Add optional simple particle feedback if feasible without destabilizing the core flow.
- `RF14`: Add a NeoForge common config file.
- `RF15`: Detect the configured Minecraft use/place keybind on the client.
- `RF16`: Send held-state updates from client to server using NeoForge custom payload networking.
- `RF17`: Advance delayed placement only while the server sees the use/place input as held.
- `RF18`: Cancel delayed placement when held input becomes false.
- `RF19`: Show action bar placement progress during pending placement.
- `RF20`: Allow a small configurable grace period for initial held-state arrival after placement starts.
- `RF21`: Do not rely on client packets alone for actual placement; final placement still requires all server-side validation to pass.

## Non-Functional Requirements

- Server-authoritative behavior for held-state enforcement and final placement.
- No item consumption unless placement actually succeeds.
- No item duplication or ghost placement on cancellation.
- Avoid external dependencies.
- Preserve vanilla or modded placement logic as much as possible by using the item's placement flow instead of directly setting block states.
- Do not affect creative mode.
- Do not run client-only code on dedicated servers.

## Out Of Scope

- Custom client HUD or cursor overlay
- Ghost block previews
- Permissions or region integration
- Combat detection rules
- Per-block or per-tag custom delays
- Non-`BlockItem` world interactions such as buckets, boats, item frames, spawn eggs, flint and steel, bone meal, and similar
- Guaranteed compatibility with every modded block that performs unusual custom placement

## Acceptance Tests

1. Holding the configured use/place key while aiming at a valid target places the block after the configured delay.
2. Releasing the configured use/place key before completion cancels placement and consumes no item.
3. Rebinding the use/place key still works because the client reads Minecraft's configured `keyUse` mapping.
4. Clicking once and releasing immediately does not place the block after the delay.
5. Holding use while looking away cancels placement and consumes nothing.
6. Holding use while switching item cancels placement and consumes nothing.
7. Holding use while moving too far away cancels placement and consumes nothing.
8. In creative, placing stone remains instant.
9. Delay applies to a block in `MAIN_HAND`.
10. Delay applies to a block in `OFF_HAND`.
11. A block id listed in the blacklist bypasses the delay.
12. A namespace listed in the blacklist bypasses the delay.
13. A block tag listed in the blacklist bypasses the delay.
14. Action bar progress appears during pending placement and clears or stops after cancel/complete.
15. Dedicated server startup does not crash from client-only classes.
16. The server does not place blocks from the held-state packet alone without the normal placement attempt and server-side validation.
