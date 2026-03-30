package com.oneblocktoendall;

import com.oneblocktoendall.gui.QuestBookScreen;
import com.oneblocktoendall.gui.QuestHudRenderer;
import com.oneblocktoendall.gui.WelcomeScreen;
import com.oneblocktoendall.network.QuestSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side mod entry point. Sets up:
 * 1. HUD renderer — shows quest progress overlay (top-right corner)
 * 2. Keybinding — press J to open the quest book
 * 3. Network handler — receives quest sync packets from server
 * 4. Welcome screen — shows controls and key bindings on world join
 */
public class OneBlockModClient implements ClientModInitializer {

    /** Keybinding to open the quest book. Default: J */
    private static KeyBinding questBookKey;

    /** Track whether we've shown the welcome screen for this session. */
    private static boolean welcomeShown = false;

    /** Delay showing the welcome screen by a few ticks so the world loads first. */
    private static int welcomeDelayTicks = -1;

    @Override
    public void onInitializeClient() {
        // 1. Register the HUD renderer
        QuestHudRenderer.register();

        // 2. Register keybinding for quest book
        questBookKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.oneblocktoendall.quest_book",  // Translation key
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,                    // Default: J key
                "category.oneblocktoendall"          // Keybinding category
        ));

        // 3. Listen for quest sync packets from server
        ClientPlayNetworking.registerGlobalReceiver(QuestSyncPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        QuestHudRenderer.setCachedData(payload);

                        // Trigger welcome screen on first sync packet received
                        if (!welcomeShown) {
                            welcomeShown = true;
                            // Delay 40 ticks (2 seconds) so the world renders first
                            welcomeDelayTicks = 40;
                        }
                    });
                });

        // 4. Handle keybinding press and welcome screen delay each tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Quest book keybind
            while (questBookKey.wasPressed()) {
                openQuestBook(client);
            }

            // Welcome screen delay countdown
            if (welcomeDelayTicks > 0) {
                welcomeDelayTicks--;
            } else if (welcomeDelayTicks == 0) {
                welcomeDelayTicks = -1; // Reset so it doesn't fire again
                if (client.currentScreen == null) {
                    client.setScreen(new WelcomeScreen());
                }
            }
        });

        // 5. Reset welcome screen flag on disconnect so it shows on next world join
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            resetWelcome();
            QuestHudRenderer.setCachedData(null);
        });
    }

    /**
     * Reset the welcome flag when disconnecting, so it shows again next session.
     */
    public static void resetWelcome() {
        welcomeShown = false;
        welcomeDelayTicks = -1;
    }

    private static void openQuestBook(MinecraftClient client) {
        QuestSyncPayload data = QuestHudRenderer.getCachedData();
        client.setScreen(new QuestBookScreen(data));
    }
}
