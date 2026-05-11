package com.oneblocktoendall.gui;

import com.oneblocktoendall.network.PhaseAdvancePayload;
import com.oneblocktoendall.network.QuestSyncPayload;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class BridgeCelebrationScreen extends Screen {

    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 300;
    private final PhaseAdvancePayload data;
    private int ticksOpen = 0;

    public BridgeCelebrationScreen(PhaseAdvancePayload data) {
        super(Text.literal("Islands Merged!"));
        this.data = data;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Continue"), button -> close())
                .dimensions(centerX - 50, panelY + PANEL_HEIGHT - 30, 100, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        ticksOpen++;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xC0000000);

        int centerX = this.width / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        // Animated gold border that pulses
        float pulse = (float) (0.7 + 0.3 * Math.sin(ticksOpen * 0.15));
        int borderAlpha = (int) (255 * pulse);
        int borderColor = (borderAlpha << 24) | 0xFFD700;

        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF0D0D1A);
        context.drawBorder(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, borderColor);
        context.drawBorder(panelX + 1, panelY + 1, PANEL_WIDTH - 2, PANEL_HEIGHT - 2, 0xFF554400);

        int y = panelY + 14;

        // Title
        context.drawCenteredTextWithShadow(textRenderer,
                "★ ISLANDS MERGED! ★", centerX, y, 0xFFFFD700);
        y += 14;
        context.drawCenteredTextWithShadow(textRenderer,
                "Phase " + data.newPhase() + ": " + data.phaseName(), centerX, y, 0xFFAAAAAA);
        y += 16;
        context.fill(panelX + 10, y, panelX + PANEL_WIDTH - 10, y + 1, 0xFFFFD700);
        y += 10;

        // Alliance quests unlocked message
        context.drawCenteredTextWithShadow(textRenderer,
                "Team quests are now active!", centerX, y, 0xFFFF88FF);
        y += 16;

        // New blocks
        if (!data.newBlockNames().isEmpty()) {
            context.drawTextWithShadow(textRenderer, "New Blocks:", panelX + 15, y, 0xFF55FF55);
            y += 12;
            for (String block : data.newBlockNames()) {
                if (y > panelY + PANEL_HEIGHT - 80) break;
                String displayName = block.replace("minecraft:", "").replace("_", " ");
                context.drawTextWithShadow(textRenderer, "  + " + displayName, panelX + 20, y, 0xFFCCCCCC);
                y += 10;
            }
            y += 4;
        }

        // Alliance quests
        if (!data.newAllianceQuests().isEmpty()) {
            context.drawTextWithShadow(textRenderer, "Alliance Quests (each member):", panelX + 15, y, 0xFFFF8844);
            y += 12;
            for (QuestSyncPayload.QuestStatus quest : data.newAllianceQuests()) {
                if (y > panelY + PANEL_HEIGHT - 60) break;
                context.drawTextWithShadow(textRenderer, "  ★ " + quest.name(), panelX + 20, y, 0xFFCCCCCC);
                y += 10;
            }
            y += 4;
        }

        // Co-op quests
        if (!data.newCoopQuests().isEmpty()) {
            context.drawTextWithShadow(textRenderer, "Co-op Quests (team effort):", panelX + 15, y, 0xFF55AAFF);
            y += 12;
            for (QuestSyncPayload.QuestStatus quest : data.newCoopQuests()) {
                if (y > panelY + PANEL_HEIGHT - 50) break;
                context.drawTextWithShadow(textRenderer, "  ✦ " + quest.name(), panelX + 20, y, 0xFFCCCCCC);
                y += 10;
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean shouldPause() { return false; }
}
