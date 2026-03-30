package com.oneblocktoendall;

import com.oneblocktoendall.config.ModConfig;
import com.oneblocktoendall.gui.*;
import com.oneblocktoendall.network.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class OneBlockModClient implements ClientModInitializer {

    private static KeyBinding questBookKey;
    private static boolean welcomeShown = false;
    private static int welcomeDelayTicks = -1;
    private static boolean playerStarted = false;

    @Override
    public void onInitializeClient() {
        // 0. Load client config
        ModConfig.load();

        // 1. Register the HUD renderer
        QuestHudRenderer.register();

        // 2. Register keybinding for quest book
        questBookKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.oneblocktoendall.quest_book",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "category.oneblocktoendall"
        ));

        // 3. Listen for quest sync packets
        ClientPlayNetworking.registerGlobalReceiver(QuestSyncPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        QuestHudRenderer.setCachedData(payload);
                        playerStarted = payload.currentPhase() > 0;

                        if (!welcomeShown) {
                            welcomeShown = true;
                            welcomeDelayTicks = 40;
                        }
                    });
                });

        // 4. Toast notification receiver
        ClientPlayNetworking.registerGlobalReceiver(ToastPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        if (!ModConfig.get().toastsEnabled) return;
                        MinecraftClient mc = MinecraftClient.getInstance();
                        switch (payload.toastType()) {
                            case ToastPayload.TYPE_QUEST ->
                                    mc.getToastManager().add(new QuestCompleteToast(payload.questName()));
                            case ToastPayload.TYPE_PHASE ->
                                    mc.getToastManager().add(new PhaseCompleteToast(
                                            payload.phaseName(), payload.phaseNumber(), payload.playerName(), false));
                            case ToastPayload.TYPE_PHASE_BROADCAST ->
                                    mc.getToastManager().add(new PhaseCompleteToast(
                                            payload.phaseName(), payload.phaseNumber(), payload.playerName(), true));
                        }
                    });
                });

        // 5. Phase advance celebration screen
        ClientPlayNetworking.registerGlobalReceiver(PhaseAdvancePayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        MinecraftClient.getInstance().setScreen(new PhaseCelebrationScreen(payload));
                    });
                });

        // 6. Block pool data
        ClientPlayNetworking.registerGlobalReceiver(BlockPoolPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        MinecraftClient.getInstance().setScreen(new BlockPoolScreen(payload));
                    });
                });

        // 7. Stats data
        ClientPlayNetworking.registerGlobalReceiver(StatsPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        MinecraftClient.getInstance().setScreen(new StatisticsScreen(payload));
                    });
                });

        // 8. Leaderboard data
        ClientPlayNetworking.registerGlobalReceiver(LeaderboardPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        MinecraftClient.getInstance().setScreen(new LeaderboardScreen(payload));
                    });
                });

        // 9. Island list data
        ClientPlayNetworking.registerGlobalReceiver(IslandListPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        MinecraftClient.getInstance().setScreen(new IslandMenuScreen(payload));
                    });
                });

        // 10. Teleport response
        ClientPlayNetworking.registerGlobalReceiver(TeleportResponsePayload.ID,
                (payload, context) -> {
                    // Message handled server-side via chat
                });

        // 11. Admin data
        ClientPlayNetworking.registerGlobalReceiver(AdminDataPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        MinecraftClient.getInstance().setScreen(new AdminScreen(payload));
                    });
                });

        // 12. Team data
        ClientPlayNetworking.registerGlobalReceiver(TeamDataPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        MinecraftClient.getInstance().setScreen(new TeamScreen(payload));
                    });
                });

        // 13. Handle keybinding + welcome/start screen delay
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (questBookKey.wasPressed()) {
                openQuestBook(client);
            }

            if (welcomeDelayTicks > 0) {
                welcomeDelayTicks--;
            } else if (welcomeDelayTicks == 0) {
                welcomeDelayTicks = -1;
                if (client.currentScreen == null) {
                    if (playerStarted) {
                        client.setScreen(new WelcomeScreen());
                    } else {
                        client.setScreen(new StartScreen());
                    }
                }
            }
        });

        // 14. Reset on disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            resetWelcome();
            QuestHudRenderer.setCachedData(null);
            playerStarted = false;
        });
    }

    public static void resetWelcome() {
        welcomeShown = false;
        welcomeDelayTicks = -1;
    }

    private static void openQuestBook(MinecraftClient client) {
        QuestSyncPayload data = QuestHudRenderer.getCachedData();
        client.setScreen(new QuestBookScreen(data));
    }
}
