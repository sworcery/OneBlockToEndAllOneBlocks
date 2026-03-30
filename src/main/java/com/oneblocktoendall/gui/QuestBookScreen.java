package com.oneblocktoendall.gui;

import com.oneblocktoendall.network.QuestSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Full-screen quest book GUI. Shows all phases, quests, and detailed progress.
 * Opened via a keybind (default: J) or the quest book item.
 *
 * Layout:
 * - Top: Paginated phase tabs with arrow navigation (7 per page)
 * - Center panel: Quests for selected phase with descriptions and progress
 * - Footer: Phase X / Y progress counter
 */
public class QuestBookScreen extends Screen {

    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 200;
    private static final int TABS_PER_PAGE = 7;

    private final QuestSyncPayload data;
    private int selectedPhase;
    private int tabPage;

    public QuestBookScreen(QuestSyncPayload data) {
        super(Text.literal("Quest Book"));
        this.data = data;
        this.selectedPhase = data != null ? data.currentPhase() : 1;
        this.tabPage = 0;
        // Start on the page that contains the current phase
        if (data != null) {
            this.tabPage = (data.currentPhase() - 1) / TABS_PER_PAGE;
        }
    }

    @Override
    protected void init() {
        super.init();

        if (data == null) return;

        int centerX = this.width / 2;
        int topY = (this.height - PANEL_HEIGHT) / 2 - 25;
        int maxPhase = data.currentPhase();
        int totalPages = (maxPhase + TABS_PER_PAGE - 1) / TABS_PER_PAGE;

        // Clamp tabPage to valid range
        tabPage = Math.max(0, Math.min(tabPage, totalPages - 1));

        int startPhase = tabPage * TABS_PER_PAGE + 1;
        int endPhase = Math.min(startPhase + TABS_PER_PAGE - 1, maxPhase);

        // Calculate how many tabs + arrows we need to center
        int tabCount = endPhase - startPhase + 1;
        boolean hasLeftArrow = tabPage > 0;
        boolean hasRightArrow = tabPage < totalPages - 1;
        int totalWidth = tabCount * 35
                + (hasLeftArrow ? 25 : 0)
                + (hasRightArrow ? 25 : 0);
        int currentX = centerX - totalWidth / 2;

        // Left arrow button
        if (hasLeftArrow) {
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> {
                tabPage--;
                clearAndInit();
            }).dimensions(currentX, topY, 20, 20).build());
            currentX += 25;
        }

        // Phase tab buttons
        for (int i = startPhase; i <= endPhase; i++) {
            final int phase = i;
            String label = "P" + i;
            addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> {
                selectedPhase = phase;
            }).dimensions(currentX, topY, 30, 20).build());
            currentX += 35;
        }

        // Right arrow button
        if (hasRightArrow) {
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> {
                tabPage++;
                clearAndInit();
            }).dimensions(currentX, topY, 20, 20).build());
        }

        // Navigation bar at the bottom of the panel
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = (this.height / 2) - PANEL_HEIGHT / 2;
        int navY = panelY + PANEL_HEIGHT + 6;
        int navBtnWidth = 42;
        int navSpacing = 3;
        String[] navLabels = {"Drops", "Stats", "Board", "Islands", "Team", "Gear"};
        int totalNavWidth = navLabels.length * navBtnWidth + (navLabels.length - 1) * navSpacing;
        int navStartX = centerX - totalNavWidth / 2;

        for (int i = 0; i < navLabels.length; i++) {
            final int idx = i;
            addDrawableChild(ButtonWidget.builder(Text.literal(navLabels[i]), button -> {
                onNavButton(idx);
            }).dimensions(navStartX + i * (navBtnWidth + navSpacing), navY, navBtnWidth, 16).build());
        }
    }

    private void onNavButton(int index) {
        if (client == null) return;
        switch (index) {
            case 0 -> { // Drops
                if (data != null) {
                    ClientPlayNetworking.send(
                            new com.oneblocktoendall.network.BlockPoolRequestPayload(data.currentPhase()));
                }
            }
            case 1 -> { // Stats
                ClientPlayNetworking.send(new com.oneblocktoendall.network.StatsRequestPayload());
            }
            case 2 -> { // Leaderboard
                ClientPlayNetworking.send(new com.oneblocktoendall.network.LeaderboardRequestPayload());
            }
            case 3 -> { // Islands
                ClientPlayNetworking.send(new com.oneblocktoendall.network.IslandListRequestPayload());
            }
            case 4 -> { // Team
                ClientPlayNetworking.send(new com.oneblocktoendall.network.TeamActionPayload("VIEW", ""));
            }
            case 5 -> client.setScreen(new SettingsScreen(this)); // Settings
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark overlay without blur (1.21.4's renderBackground() adds unwanted blur)
        context.fill(0, 0, this.width, this.height, 0xA0000000);

        if (data == null) {
            context.drawCenteredTextWithShadow(textRenderer,
                    "No quest data — use /oneblock start first!",
                    this.width / 2, this.height / 2, 0xFFFF5555);
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = centerY - PANEL_HEIGHT / 2;

        // Main panel background — solid dark blue, fully opaque
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT,
                0xFF1A1A2E);
        context.drawBorder(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFFFFD700); // Gold border

        // Phase title
        String phaseTitle;
        boolean isCurrentPhase = (selectedPhase == data.currentPhase());

        if (isCurrentPhase) {
            phaseTitle = "Phase " + data.currentPhase() + ": " + data.phaseName();
        } else {
            phaseTitle = "Phase " + selectedPhase + " (Completed)";
        }

        context.drawCenteredTextWithShadow(textRenderer, phaseTitle,
                centerX, panelY + 10, 0xFFFFD700);

        // Divider line
        context.fill(panelX + 10, panelY + 25, panelX + PANEL_WIDTH - 10, panelY + 26,
                0xFF666666);

        // Quest list
        int questY = panelY + 35;
        int lineHeight = 14;
        int barWidth = 150;
        int barHeight = 8;

        if (isCurrentPhase && data.quests() != null) {
            for (QuestSyncPayload.QuestStatus quest : data.quests()) {
                // Checkmark or bullet
                String prefix = quest.completed() ? "\u2714 " : "\u2022 ";
                int nameColor = quest.completed() ? 0xFF55FF55 : 0xFFFFFFFF;

                // Quest name
                context.drawTextWithShadow(textRenderer,
                        prefix + quest.name(), panelX + 15, questY, nameColor);

                // Description
                context.drawTextWithShadow(textRenderer,
                        quest.description(), panelX + 25, questY + lineHeight,
                        0xFF999999);

                // Progress bar (for incomplete quests)
                if (!quest.completed()) {
                    int barX = panelX + 25;
                    int barY = questY + lineHeight * 2;

                    // Background
                    context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);

                    // Fill
                    float percent = (float) quest.progress() / quest.required();
                    int fillWidth = (int) (barWidth * percent);
                    if (fillWidth > 0) {
                        int barColor = percent >= 0.75f ? 0xFF55FF55 : 0xFFFFFF55;
                        context.fill(barX, barY, barX + fillWidth, barY + barHeight, barColor);
                    }

                    // Border
                    context.drawBorder(barX, barY, barWidth, barHeight, 0xFF666666);

                    // Progress text
                    String progressStr = quest.progress() + " / " + quest.required();
                    context.drawTextWithShadow(textRenderer, progressStr,
                            barX + barWidth + 8, barY - 1, 0xFFCCCCCC);

                    questY += lineHeight * 2 + barHeight + 8;
                } else {
                    questY += lineHeight * 2 + 4;
                }
            }
        } else if (!isCurrentPhase) {
            context.drawTextWithShadow(textRenderer,
                    "All quests in this phase were completed!",
                    panelX + 15, questY, 0xFF55FF55);
        }

        // Footer with phase progress (uses dynamic maxPhase from server)
        String footer = "Phase " + data.currentPhase() + " / " + data.maxPhase();
        context.drawCenteredTextWithShadow(textRenderer, footer,
                centerX, panelY + PANEL_HEIGHT - 15, 0xFF888888);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false; // Don't pause the game when quest book is open
    }
}
