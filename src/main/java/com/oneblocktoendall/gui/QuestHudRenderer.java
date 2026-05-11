package com.oneblocktoendall.gui;

import com.oneblocktoendall.config.ModConfig;
import com.oneblocktoendall.network.QuestSyncPayload;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.List;

public class QuestHudRenderer {

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

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getDebugHud().shouldShowDebugHud()) return;
        if (client.currentScreen != null) return;

        ModConfig config = ModConfig.get();
        if (!config.hudEnabled) return;

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        float scale = config.hudScale;
        int panelWidth = 160;

        int questCount = countIncomplete(cachedData.quests());
        int allianceCount = cachedData.isMergedTeam() ? countIncomplete(cachedData.allianceQuests()) : 0;
        int coopCount = cachedData.isMergedTeam() ? countIncomplete(cachedData.coopQuests()) : 0;
        int totalIncomplete = questCount + allianceCount + coopCount;

        int estimatedHeight = 20 + (totalIncomplete * (12 + 6 + 4)) + 5;
        if (cachedData.isMergedTeam() && (allianceCount > 0 || coopCount > 0)) {
            estimatedHeight += 28;
        }
        if (totalIncomplete == 0) estimatedHeight = 35;

        int x = config.hudPosition.getX((int)(screenWidth / scale), panelWidth);
        int y = config.hudPosition.getY((int)(screenHeight / scale), estimatedHeight);

        if (scale != 1.0f) {
            context.getMatrices().push();
            context.getMatrices().scale(scale, scale, 1.0f);
        }

        int panelHeight = estimatedHeight;
        context.fill(x - 5, y - 5, x + panelWidth + 5, y + panelHeight, 0x80000000);

        String header = "Phase " + cachedData.currentPhase() + ": " + cachedData.phaseName();
        context.drawText(textRenderer, header, x, y, 0xFFD700, true);
        y += 16;

        if (totalIncomplete == 0) {
            context.drawText(textRenderer, "All quests complete!", x, y, 0x55FF55, true);
            if (scale != 1.0f) context.getMatrices().pop();
            return;
        }

        y = renderQuestList(context, textRenderer, x, y, cachedData.quests(), 0xFFFFFF);

        if (cachedData.isMergedTeam()) {
            if (!cachedData.allianceQuests().isEmpty() && allianceCount > 0) {
                context.drawText(textRenderer, "Alliance:", x, y, 0xFF88FF, true);
                y += 12;
                y = renderQuestList(context, textRenderer, x, y, cachedData.allianceQuests(), 0xFFCCFF);
            }
            if (!cachedData.coopQuests().isEmpty() && coopCount > 0) {
                context.drawText(textRenderer, "Co-op:", x, y, 0x55AAFF, true);
                y += 12;
                y = renderQuestList(context, textRenderer, x, y, cachedData.coopQuests(), 0xAADDFF);
            }
        }

        if (scale != 1.0f) context.getMatrices().pop();
    }

    private static int renderQuestList(DrawContext context, TextRenderer textRenderer,
                                        int x, int y, List<QuestSyncPayload.QuestStatus> quests,
                                        int textColor) {
        int barWidth = 80;
        int barHeight = 6;

        for (QuestSyncPayload.QuestStatus quest : quests) {
            if (quest.completed()) continue;

            String progressText = quest.name() + " " + quest.progress() + "/" + quest.required();
            context.drawText(textRenderer, progressText, x, y, textColor, true);
            y += 12;

            context.fill(x, y, x + barWidth, y + barHeight, 0xFF333333);

            float percent = (float) quest.progress() / quest.required();
            int fillWidth = (int) (barWidth * percent);
            if (fillWidth > 0) {
                int color = percent >= 0.75f ? 0xFF55FF55 :
                            percent >= 0.5f  ? 0xFFFFFF55 :
                                               0xFFFF8844;
                context.fill(x, y, x + fillWidth, y + barHeight, color);
            }

            context.drawBorder(x, y, barWidth, barHeight, 0xFF666666);
            y += barHeight + 4;
        }
        return y;
    }

    private static int countIncomplete(List<QuestSyncPayload.QuestStatus> quests) {
        return (int) quests.stream().filter(q -> !q.completed()).count();
    }
}
