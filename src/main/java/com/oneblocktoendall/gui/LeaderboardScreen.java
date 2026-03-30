package com.oneblocktoendall.gui;

import com.oneblocktoendall.network.LeaderboardPayload;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class LeaderboardScreen extends Screen {

    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 260;
    private final LeaderboardPayload data;

    public LeaderboardScreen(LeaderboardPayload data) {
        super(Text.literal("Leaderboard"));
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
        context.drawCenteredTextWithShadow(textRenderer, "Leaderboard", centerX, y, 0xFFFFD700);
        y += 16;
        context.fill(panelX + 10, y, panelX + PANEL_WIDTH - 10, y + 1, 0xFF666666);
        y += 8;

        // Column headers
        int col1 = panelX + 15;
        int col2 = panelX + 40;
        int col3 = panelX + 160;
        int col4 = panelX + 210;
        int col5 = panelX + 260;

        context.drawTextWithShadow(textRenderer, "#", col1, y, 0xFFFFD700);
        context.drawTextWithShadow(textRenderer, "Player", col2, y, 0xFFFFD700);
        context.drawTextWithShadow(textRenderer, "Phase", col3, y, 0xFFFFD700);
        context.drawTextWithShadow(textRenderer, "Quests", col4, y, 0xFFFFD700);
        context.drawTextWithShadow(textRenderer, "Blocks", col5, y, 0xFFFFD700);
        y += 14;

        context.fill(panelX + 10, y - 2, panelX + PANEL_WIDTH - 10, y - 1, 0xFF444444);

        for (int i = 0; i < data.entries().size(); i++) {
            if (y > panelY + PANEL_HEIGHT - 40) break;
            LeaderboardPayload.LeaderboardEntry entry = data.entries().get(i);
            boolean isMe = entry.name().equals(data.requestingPlayer());
            int color = isMe ? 0xFFFFD700 : 0xFFCCCCCC;

            // Rank medal colors
            int rankColor = switch (i) {
                case 0 -> 0xFFFFD700; // Gold
                case 1 -> 0xFFC0C0C0; // Silver
                case 2 -> 0xFFCD7F32; // Bronze
                default -> color;
            };

            context.drawTextWithShadow(textRenderer, String.valueOf(i + 1), col1, y, rankColor);
            context.drawTextWithShadow(textRenderer, entry.name(), col2, y, color);
            context.drawTextWithShadow(textRenderer, String.valueOf(entry.phase()), col3, y, color);
            context.drawTextWithShadow(textRenderer, String.valueOf(entry.questsCompleted()), col4, y, color);
            context.drawTextWithShadow(textRenderer, String.valueOf(entry.blocksBroken()), col5, y, color);

            if (isMe) {
                context.drawBorder(panelX + 12, y - 2, PANEL_WIDTH - 24, 14, 0x40FFD700);
            }
            y += 14;
        }

        if (data.entries().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer,
                    "No players have started yet!", centerX, y + 20, 0xFF888888);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Skip default blur — we draw our own dark overlay in render()
    }

    @Override
    public boolean shouldPause() { return false; }
}
