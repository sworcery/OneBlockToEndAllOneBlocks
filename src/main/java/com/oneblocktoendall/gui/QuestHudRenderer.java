package com.oneblocktoendall.gui;

import com.oneblocktoendall.config.ModConfig;
import com.oneblocktoendall.network.QuestSyncPayload;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Renders quest objectives as a small overlay in the top-right corner of the screen.
 * Shows the current phase name and active (incomplete) quests with progress bars.
 *
 * Data comes from QuestSyncPayload sent by the server, cached in ClientQuestData.
 */
public class QuestHudRenderer {

    /** Cached quest data received from the server. Null if no data yet. */
    private static QuestSyncPayload cachedData = null;

    public static void setCachedData(QuestSyncPayload data) {
        cachedData = data;
    }

    public static QuestSyncPayload getCachedData() {
        return cachedData;
    }

    public static void register() {
        HudRenderCallback.EVENT.register(QuestHudRenderer::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (cachedData == null) return;
        if (!ModConfig.get().hudEnabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getDebugHud().shouldShowDebugHud()) return;
        if (client.currentScreen != null) return;

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        float scale = ModConfig.get().hudScale;
        int panelWidth = 160;

        // Estimate panel height for position calculation
        int questCount = (int) cachedData.quests().stream()
                .filter(q -> !q.completed()).count();
        int estimatedHeight = 20 + (questCount * (12 + 6 + 4)) + 5;
        if (questCount == 0) estimatedHeight = 35;

        int x = ModConfig.get().hudPosition.getX((int)(screenWidth / scale), panelWidth);
        int y = ModConfig.get().hudPosition.getY((int)(screenHeight / scale), estimatedHeight);

        if (scale != 1.0f) {
            context.getMatrices().push();
            context.getMatrices().scale(scale, scale, 1.0f);
        }
        int lineHeight = 12;
        int barWidth = 80;
        int barHeight = 6;
        int panelHeight = estimatedHeight;

        context.fill(x - 5, y - 5, x + panelWidth + 5, y + panelHeight,
                0x80000000); // Semi-transparent black

        // Phase header
        String header = "Phase " + cachedData.currentPhase() + ": " + cachedData.phaseName();
        context.drawText(textRenderer, header, x, y, 0xFFD700, true); // Gold color
        y += lineHeight + 4;

        if (questCount == 0) {
            context.drawText(textRenderer, "All quests complete!", x, y, 0x55FF55, true);
            if (scale != 1.0f) context.getMatrices().pop();
            return;
        }

        // Draw each incomplete quest
        for (QuestSyncPayload.QuestStatus quest : cachedData.quests()) {
            if (quest.completed()) continue;

            // Quest name + progress text
            String progressText = quest.name() + " " + quest.progress() + "/" + quest.required();
            context.drawText(textRenderer, progressText, x, y, 0xFFFFFF, true);
            y += lineHeight;

            // Progress bar background (dark gray)
            context.fill(x, y, x + barWidth, y + barHeight, 0xFF333333);

            // Progress bar fill (green gradient based on %)
            float percent = (float) quest.progress() / quest.required();
            int fillWidth = (int) (barWidth * percent);
            if (fillWidth > 0) {
                int color = percent >= 0.75f ? 0xFF55FF55 :   // Green when close
                            percent >= 0.5f  ? 0xFFFFFF55 :   // Yellow at halfway
                                               0xFFFF8844;     // Orange early on
                context.fill(x, y, x + fillWidth, y + barHeight, color);
            }

            // Progress bar border
            context.drawBorder(x, y, barWidth, barHeight, 0xFF666666);

            y += barHeight + 4;
        }

        if (scale != 1.0f) context.getMatrices().pop();
    }
}
