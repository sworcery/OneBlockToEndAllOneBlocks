package com.oneblocktoendall.gui;

import com.oneblocktoendall.network.StatsPayload;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class StatisticsScreen extends Screen {

    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 260;
    private final StatsPayload data;

    public StatisticsScreen(StatsPayload data) {
        super(Text.literal("Statistics"));
        this.data = data;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> close())
                .dimensions(centerX - 40, panelY + PANEL_HEIGHT - 28, 80, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xA0000000);

        int centerX = this.width / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF1A1A2E);
        context.drawBorder(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFFFFD700);

        int y = panelY + 10;
        context.drawCenteredTextWithShadow(textRenderer, "Challenge Statistics", centerX, y, 0xFFFFD700);
        y += 16;
        context.fill(panelX + 10, y, panelX + PANEL_WIDTH - 10, y + 1, 0xFF666666);
        y += 10;

        // Summary stats
        drawStat(context, panelX + 20, y, "Current Phase", data.currentPhase() + " / " + data.maxPhase());
        y += 14;
        drawStat(context, panelX + 20, y, "Blocks Broken", String.valueOf(data.totalBlocksBroken()));
        y += 14;
        drawStat(context, panelX + 20, y, "Quests Completed", String.valueOf(data.totalQuestsCompleted()));
        y += 14;

        // Play time
        long elapsedTicks = data.currentTime() - data.challengeStartTime();
        long seconds = elapsedTicks / 20;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        String timeStr = hours > 0
                ? String.format("%dh %dm %ds", hours, minutes, secs)
                : String.format("%dm %ds", minutes, secs);
        drawStat(context, panelX + 20, y, "Time Played", timeStr);
        y += 20;

        // Phase breakdown
        context.fill(panelX + 10, y, panelX + PANEL_WIDTH - 10, y + 1, 0xFF666666);
        y += 8;
        context.drawTextWithShadow(textRenderer, "Phase Breakdown:", panelX + 15, y, 0xFFFFD700);
        y += 14;

        for (StatsPayload.PhaseStats ps : data.phaseStats()) {
            if (y > panelY + PANEL_HEIGHT - 40) break;
            String status = ps.questsDone() == ps.questsTotal() ? "\u2714" : "\u2022";
            int color = ps.questsDone() == ps.questsTotal() ? 0xFF55FF55 : 0xFFFFFFFF;
            context.drawTextWithShadow(textRenderer,
                    status + " P" + ps.phase() + ": " + ps.name() +
                            " (" + ps.questsDone() + "/" + ps.questsTotal() + ")",
                    panelX + 20, y, color);
            y += 12;
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawStat(DrawContext context, int x, int y, String label, String value) {
        context.drawTextWithShadow(textRenderer, label + ":", x, y, 0xFFAAAAAA);
        context.drawTextWithShadow(textRenderer, value, x + 130, y, 0xFFFFFFFF);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Skip default blur — we draw our own dark overlay in render()
    }

    @Override
    public boolean shouldPause() { return false; }
}
