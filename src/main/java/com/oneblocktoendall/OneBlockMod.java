package com.oneblocktoendall;

import com.oneblocktoendall.block.ModBlocks;
import com.oneblocktoendall.command.OneBlockCommand;
import com.oneblocktoendall.event.OneBlockTickHandler;
import com.oneblocktoendall.network.ModNetworking;
import com.oneblocktoendall.phase.PhaseManager;
import com.oneblocktoendall.quest.QuestManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main mod entry point (runs on both client and server).
 *
 * Initialization order matters:
 * 1. Blocks — Register the one block with Minecraft's registry
 * 2. Networking — Register packet types before anything tries to send them
 * 3. Phases — Load phase definitions from JSON
 * 4. Quests — Initialize quest manager
 * 5. Commands — Register /oneblock command
 * 6. Tick Handler — Start the server tick loop for regeneration & quest checking
 */
public class OneBlockMod implements ModInitializer {

    public static final String MOD_ID = "oneblocktoendall";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing OneBlockToRuleThemAll...");

        // 1. Register blocks (the one block itself)
        ModBlocks.register();

        // 2. Register network packet types
        ModNetworking.registerS2CPackets();

        // 3. Load phase definitions from phases.json
        PhaseManager.init();

        // 4. Initialize quest system
        QuestManager.init();

        // 5. Register /oneblock command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            OneBlockCommand.register(dispatcher);
        });

        // 6. Start tick handler (block regeneration + quest checking)
        OneBlockTickHandler.register();

        // 7. Auto-start challenge when a player joins for the first time
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Runs after the player entity is fully loaded into the world
            server.execute(() -> {
                OneBlockCommand.initializeChallenge(handler.getPlayer());
            });
        });

        LOGGER.info("OneBlockToRuleThemAll initialized! {} phases loaded.", PhaseManager.getMaxPhase());
    }
}
