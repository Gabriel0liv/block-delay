# Delayed Placement MVP Tasks

1. Add common config values for delay, survival-only mode, max distance, debug flag, particles flag, and blacklists.
2. Replace template mod metadata and generated example content with real delayed-placement mod metadata.
3. Add `PendingPlacement` model.
4. Add `PendingPlacementManager` keyed by player UUID.
5. Add a server-side placement-attempt event handler for `BlockItem` use on blocks.
6. Add server tick progression for pending placements.
7. Add validation and cancellation reasons for item, hand, target, distance, dimension, survival state, and death.
8. Add blacklist checks for block id, namespace, and block tag id.
9. Add bypass mechanism so final placement does not cancel itself.
10. Add debug logging at attempt, progress, cancel, bypass, and complete points.
11. Add optional basic particle hook only if low-risk after the core flow compiles.
12. Build with Gradle, fix compile errors, and run a manual test checklist.
