package com.oneblocktoendall;

import com.oneblocktoendall.block.ModBlocks;
import com.oneblocktoendall.command.OneBlockCommand;
import com.oneblocktoendall.config.ServerConfig;
import com.oneblocktoendall.data.OneBlockWorldState;
import com.oneblocktoendall.event.OneBlockTickHandler;
import com.oneblocktoendall.network.ModNetworking;
import com.oneblocktoendall.phase.PhaseManager;
import com.oneblocktoendall.quest.PlayerProgress;
import com.oneblocktoendall.quest.QuestManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OneBlockMod implements ModInitializer {

    public static final String MOD_ID = "oneblocktoendall";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing OneBlockToRuleThemAll...");

        // 1. Register blocks
        ModBlocks.register();

        // 2. Register network packet types (S2C + C2S)
        ModNetworking.registerS2CPackets();
        ModNetworking.registerC2SPackets();

        // 3. Load phase definitions
        PhaseManager.init();

        // 4. Initialize quest system
        QuestManager.init();

        // 5. Load server config
        ServerConfig.load();

        // 6. Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            OneBlockCommand.register(dispatcher);
        });

        // 7. Start tick handler
        OneBlockTickHandler.register();

        // 8. Handle player join - auto-start or opt-in based on config
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            server.execute(() -> {
                OneBlockWorldState state = OneBlockWorldState.get(server);
                PlayerProgress progress = state.getOrCreateProgress(handler.getPlayer().getUuid());

                if (ServerConfig.get().autoStart) {
                    // Legacy auto-start behavior
                    OneBlockCommand.initializeChallenge(handler.getPlayer());
                }
                // If not auto-start, client will show StartScreen via QuestSyncPayload check
                // Send initial sync so client knows the player's state
                if (progress.isStarted()) {
                    ModNetworking.syncQuestProgress(handler.getPlayer(), progress);
                }
            });
        });

        LOGGER.info("OneBlockToRuleThemAll initialized! {} phases loaded.", PhaseManager.getMaxPhase());
    }
}
