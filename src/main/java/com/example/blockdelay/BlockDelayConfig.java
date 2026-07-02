package com.example.blockdelay;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class BlockDelayConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue PLACEMENT_DELAY_TICKS = BUILDER
            .comment("How many server ticks a survival player must hold a valid block placement target before the block is placed.")
            .defineInRange("placementDelayTicks", 12, 1, 20 * 60);

    public static final ModConfigSpec.BooleanValue ONLY_SURVIVAL = BUILDER
            .comment("If true, delayed placement only applies to survival players.")
            .define("onlySurvival", true);

    public static final ModConfigSpec.DoubleValue MAX_DISTANCE_FROM_TARGET = BUILDER
            .comment("Maximum eye-to-target distance allowed while a placement is pending.")
            .defineInRange("maxDistanceFromTarget", 5.0D, 1.0D, 64.0D);

    public static final ModConfigSpec.BooleanValue ENABLE_DEBUG_LOGGING = BUILDER
            .comment("Enable debug logging for delayed placement attempts, progress, cancellations, and completions.")
            .define("enableDebugLogging", true);

    public static final ModConfigSpec.BooleanValue SHOW_PARTICLES = BUILDER
            .comment("If true, the server emits simple particles while a placement is pending.")
            .define("showParticles", false);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_BLOCKS = BUILDER
            .comment("Block ids that bypass the placement delay, for example minecraft:torch.")
            .defineListAllowEmpty("blacklistedBlocks", List.of(), () -> "", BlockDelayConfig::isResourceLocation);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_MODS = BUILDER
            .comment("Block namespaces that bypass the placement delay, for example create.")
            .defineListAllowEmpty("blacklistedMods", List.of(), () -> "", BlockDelayConfig::isNamespace);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_BLOCK_TAGS = BUILDER
            .comment("Block tag ids that bypass the placement delay, for example myserver:instant_place.")
            .defineListAllowEmpty("blacklistedBlockTags", List.of(), () -> "", BlockDelayConfig::isResourceLocation);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private BlockDelayConfig() {
    }

    private static boolean isResourceLocation(Object value) {
        return value instanceof String stringValue && ResourceLocation.tryParse(stringValue) != null;
    }

    private static boolean isNamespace(Object value) {
        if (!(value instanceof String stringValue)) {
            return false;
        }

        return !stringValue.isBlank() && stringValue.equals(stringValue.toLowerCase(java.util.Locale.ROOT));
    }
}
