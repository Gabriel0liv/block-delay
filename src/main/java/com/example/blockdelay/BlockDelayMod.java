package com.example.blockdelay;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(BlockDelayMod.MODID)
public final class BlockDelayMod {
    public static final String MODID = "blockdelay";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BlockDelayMod(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.addListener(DelayedPlacementHandler::onUseItemOnBlock);
        NeoForge.EVENT_BUS.addListener(DelayedPlacementHandler::onPlayerTickPost);
        NeoForge.EVENT_BUS.addListener(DelayedPlacementHandler::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(DelayedPlacementHandler::onPlayerChangedDimension);

        modContainer.registerConfig(ModConfig.Type.COMMON, BlockDelayConfig.SPEC);
    }
}
