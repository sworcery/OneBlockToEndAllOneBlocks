package com.oneblocktoendall.gui;

import com.oneblocktoendall.network.QuestSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class QuestBookScreen extends Screen {

    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 240;
    private static final int TABS_PER_PAGE = 7;

    private final QuestSyncPayload data;
    private int selectedPhase;
    private int tabPage;
    private int scrollOffset = 0;

    public QuestBookScreen(QuestSyncPayload data) {
        super(Text.literal("Quest Book"));
        this.data = data;
        this.selectedPhase = data != null ? data.currentPhase() : 1;
        this.tabPage = 0;
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
        int unlockedPhases = data.currentPhase();
        int totalPages = (unlockedPhases + TABS_PER_PAGE - 1) / TABS_PER_PAGE;

        tabPage = Math.max(0, Math.min(tabPage, totalPages - 1));

        int startPhase = tabPage * TABS_PER_PAGE + 1;
        int endPhase = Math.min(startPhase + TABS_PER_PAGE - 1, unlockedPhases);

        int tabCount = endPhase - startPhase + 1;
        boolean hasLeftArrow = tabPage > 0;
        boolean hasRightArrow = tabPage < totalPages - 1;
        int totalWidth = tabCount * 35
                + (hasLeftArrow ? 25 : 0)
                + (hasRightArrow ? 25 : 0);
        int currentX = centerX - totalWidth / 2;

        if (hasLeftArrow) {
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> {
                tabPage--;
                clearAndInit();
            }).dimensions(currentX, topY, 20, 20).build());
            currentX += 25;
        }

        for (int i = startPhase; i <= endPhase; i++) {
            final int phase = i;
            String label = "P" + i;
            addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> {
                selectedPhase = phase;
                scrollOffset = 0;
            }).dimensions(currentX, topY, 30, 20).build());
            currentX += 35;
        }

        if (hasRightArrow) {
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> {
                tabPage++;
                clearAndInit();
            }).dimensions(currentX, topY, 20, 20).build());
        }

        // Navigation bar
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
            case 0 -> {
                if (data != null) {
                    ClientPlayNetworking.send(
                            new com.oneblocktoendall.network.BlockPoolRequestPayload(data.currentPhase()));
                }
            }
            case 1 -> ClientPlayNetworking.send(new com.oneblocktoendall.network.StatsRequestPayload());
            case 2 -> ClientPlayNetworking.send(new com.oneblocktoendall.network.LeaderboardRequestPayload());
            case 3 -> ClientPlayNetworking.send(new com.oneblocktoendall.network.IslandListRequestPayload());
            case 4 -> ClientPlayNetworking.send(new com.oneblocktoendall.network.TeamActionPayload("VIEW", ""));
            case 5 -> client.setScreen(new SettingsScreen(this));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset -= (int) verticalAmount * 14;
        scrollOffset = Math.max(0, scrollOffset);
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
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

        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF1A1A2E);
        context.drawBorder(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFFFFD700);

        boolean isCurrentPhase = (selectedPhase == data.currentPhase());

        String phaseTitle;
        if (isCurrentPhase) {
            phaseTitle = "Phase " + data.currentPhase() + ": " + data.phaseName();
        } else {
            phaseTitle = "Phase " + selectedPhase + " (Previous)";
        }

        context.drawCenteredTextWithShadow(textRenderer, phaseTitle,
                centerX, panelY + 10, 0xFFFFD700);
        context.fill(panelX + 10, panelY + 25, panelX + PANEL_WIDTH - 10, panelY + 26, 0xFF666666);

        // Enable scissor to clip quest content within the panel
        int contentTop = panelY + 30;
        int contentBottom = panelY + PANEL_HEIGHT - 20;
        context.enableScissor(panelX, contentTop, panelX + PANEL_WIDTH, contentBottom);

        int questY = contentTop + 5 - scrollOffset;

        if (isCurrentPhase && data.quests() != null) {
            questY = renderQuestSection(context, panelX, questY, contentBottom,
                    "Quests", 0xFFFFFF55, data.quests());

            if (data.isMergedTeam()) {
                if (!data.allianceQuests().isEmpty()) {
                    questY = renderQuestSection(context, panelX, questY, contentBottom,
                            "Alliance (each member)", 0xFFFF88FF, data.allianceQuests());
                }
                if (!data.coopQuests().isEmpty()) {
                    questY = renderQuestSection(context, panelX, questY, contentBottom,
                            "Co-op (team effort)", 0xFF55AAFF, data.coopQuests());
                }
            }
        } else if (!isCurrentPhase) {
            context.drawTextWithShadow(textRenderer,
                    "All quests in this phase were completed!",
                    panelX + 15, questY, 0xFF55FF55);
        }

        context.disableScissor();

        String footer = "Phase " + data.currentPhase() + " / " + data.maxPhase();
        context.drawCenteredTextWithShadow(textRenderer, footer,
                centerX, panelY + PANEL_HEIGHT - 15, 0xFF888888);

        super.render(context, mouseX, mouseY, delta);
    }

    private int renderQuestSection(DrawContext context, int panelX, int y, int maxY,
                                    String header, int headerColor,
                                    List<QuestSyncPayload.QuestStatus> quests) {
        int lineHeight = 12;
        int barWidth = 150;
        int barHeight = 6;

        context.drawTextWithShadow(textRenderer, header + ":", panelX + 12, y, headerColor);
        y += lineHeight + 2;

        for (QuestSyncPayload.QuestStatus quest : quests) {
            String prefix = quest.completed() ? "✔ " : "• ";
            int nameColor = quest.completed() ? 0xFF55FF55 : 0xFFFFFFFF;

            context.drawTextWithShadow(textRenderer, prefix + quest.name(), panelX + 15, y, nameColor);
            context.drawTextWithShadow(textRenderer, quest.description(), panelX + 25, y + lineHeight, 0xFF999999);

            if (!quest.completed()) {
                int barX = panelX + 25;
                int barY = y + lineHeight * 2;
                context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
                float percent = (float) quest.progress() / quest.required();
                int fillWidth = (int) (barWidth * percent);
                if (fillWidth > 0) {
                    int barColor = percent >= 0.75f ? 0xFF55FF55 : 0xFFFFFF55;
                    context.fill(barX, barY, barX + fillWidth, barY + barHeight, barColor);
                }
                context.drawBorder(barX, barY, barWidth, barHeight, 0xFF666666);
                context.drawTextWithShadow(textRenderer,
                        quest.progress() + " / " + quest.required(),
                        barX + barWidth + 8, barY - 1, 0xFFCCCCCC);
                y += lineHeight * 2 + barHeight + 10;
            } else {
                y += lineHeight * 2 + 6;
            }
        }

        return y + 4;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
