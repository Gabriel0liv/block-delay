# Delayed Placement MVP Spec

## Problem Statement

Instant block placement lets survival players rapidly box themselves in during combat-heavy gameplay. The server needs a server-authoritative delay for `BlockItem` placement so building remains possible, but panic placement becomes less effective.

## Current Repository Context

- Build system: Gradle with `net.neoforged.moddev` plugin
- Minecraft target: `1.21`
- NeoForge target: `21.0.167`
- Java target: `21`
- Current template metadata is still placeholder content:
  - `mod_id=examplemod`
  - package `com.example.examplemod`
  - sample item/block/config code still present

## Assumptions

- The placeholder template metadata should be replaced for the MVP.
- Because no canonical package was provided, the implementation will use `mod_id=blockdelay` and package `com.example.blockdelay` as a pragmatic replacement for the generated example package.
- The MVP will prioritize server-side correctness over client polish.

## MVP Scope

When a survival player right-clicks a block with a `BlockItem` in either hand:

1. Immediate placement is prevented.
2. A pending placement entry is stored on the server.
3. The player must remain valid and continue aiming at the same clicked block face for a configurable delay.
4. When the delay completes, the mod retries placement through the item's normal placement path.
5. If validation fails before completion, the pending placement is cancelled with no item consumption.

Important MVP limitation:

- This slice does not truly detect continuous right-click holding.
- Current behavior is: a placement attempt starts from a right-click, then completes later only if the player keeps aiming at the same clicked block/face and remains valid.
- True "must hold right click" behavior is a future improvement and would likely need client-side input tracking, networking, or another more advanced approach.

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

## Non-Functional Requirements

- Server-authoritative behavior for the delay and final placement.
- No item consumption unless placement actually succeeds.
- No item duplication or ghost placement on cancellation.
- Avoid external dependencies.
- Preserve vanilla or modded placement logic as much as possible by using the item's placement flow instead of directly setting block states.
- Do not affect creative mode.
- Do not run client-only code on dedicated servers.

## Out Of Scope

- Client progress UI or overlay
- Ghost block previews
- Permissions or region integration
- Combat detection rules
- Per-block or per-tag custom delays
- Non-`BlockItem` world interactions such as buckets, boats, item frames, spawn eggs, flint and steel, bone meal, and similar
- Guaranteed compatibility with every modded block that performs unusual custom placement

## Acceptance Tests

1. In survival, right-clicking with stone starts a delayed placement and the block appears only after the configured delay if the player keeps aiming at the same target.
2. In creative, placing stone remains instant.
3. Delay applies to a block in `MAIN_HAND`.
4. Delay applies to a block in `OFF_HAND`.
5. Switching the held item before completion cancels placement and consumes nothing.
6. Looking away before completion cancels placement and consumes nothing.
7. Moving too far away before completion cancels placement and consumes nothing.
8. A block id listed in the blacklist bypasses the delay.
9. A namespace listed in the blacklist bypasses the delay.
10. Slabs, stairs, lanterns, and similar blocks still use normal orientation/state logic after the delay when the underlying item placement succeeds.
