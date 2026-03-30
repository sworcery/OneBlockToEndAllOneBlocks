package com.oneblocktoendall.gui;

import com.oneblocktoendall.network.PhaseAdvancePayload;
import com.oneblocktoendall.network.QuestSyncPayload;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class PhaseCelebrationScreen extends Screen {

    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 260;
    private final PhaseAdvancePayload data;

    public PhaseCelebrationScreen(PhaseAdvancePayload data) {
        super(Text.literal("Phase Complete!"));
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xA0000000);

        int centerX = this.width / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF1A1A2E);
        context.drawBorder(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFFFFD700);

        int y = panelY + 12;
        context.drawCenteredTextWithShadow(textRenderer,
                "Phase " + data.newPhase() + ": " + data.phaseName(), centerX, y, 0xFFFFD700);
        y += 18;
        context.fill(panelX + 10, y, panelX + PANEL_WIDTH - 10, y + 1, 0xFF666666);
        y += 8;

        // New blocks
        if (!data.newBlockNames().isEmpty()) {
            context.drawTextWithShadow(textRenderer, "New Blocks Unlocked:", panelX + 15, y, 0xFF55FF55);
            y += 14;
            for (String block : data.newBlockNames()) {
                if (y > panelY + PANEL_HEIGHT - 50) break;
                String displayName = block.replace("minecraft:", "").replace("_", " ");
                context.drawTextWithShadow(textRenderer, "  + " + displayName, panelX + 20, y, 0xFFCCCCCC);
                y += 12;
            }
            y += 4;
        }

        // New mobs
        if (!data.newMobNames().isEmpty()) {
            context.drawTextWithShadow(textRenderer, "New Mobs:", panelX + 15, y, 0xFFFF8844);
            y += 14;
            for (String mob : data.newMobNames()) {
                if (y > panelY + PANEL_HEIGHT - 50) break;
                String displayName = mob.replace("minecraft:", "").replace("_", " ");
                context.drawTextWithShadow(textRenderer, "  + " + displayName, panelX + 20, y, 0xFFCCCCCC);
                y += 12;
            }
            y += 4;
        }

        // New quests
        if (!data.newQuests().isEmpty()) {
            context.drawTextWithShadow(textRenderer, "New Quests:", panelX + 15, y, 0xFFFFFF55);
            y += 14;
            for (QuestSyncPayload.QuestStatus quest : data.newQuests()) {
                if (y > panelY + PANEL_HEIGHT - 50) break;
                context.drawTextWithShadow(textRenderer, "  \u2022 " + quest.name(), panelX + 20, y, 0xFFCCCCCC);
                y += 12;
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}
