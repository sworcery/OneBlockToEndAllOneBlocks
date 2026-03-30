package com.oneblocktoendall.gui;

import com.oneblocktoendall.network.StartChoicePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class StartScreen extends Screen {

    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 180;

    public StartScreen() {
        super(Text.literal("One Block Challenge"));
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Start Challenge"),
                button -> {
                    ClientPlayNetworking.send(new StartChoicePayload(true));
                    close();
                }
        ).dimensions(centerX - 70, panelY + 100, 140, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Spectate"),
                button -> {
                    ClientPlayNetworking.send(new StartChoicePayload(false));
                    close();
                }
        ).dimensions(centerX - 70, panelY + 130, 140, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xA0000000);

        int centerX = this.width / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF1A1A2E);
        context.drawBorder(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFFFFD700);

        int y = panelY + 15;
        context.drawCenteredTextWithShadow(textRenderer,
                "One Block to Rule Them All", centerX, y, 0xFFFFD700);
        y += 20;
        context.fill(panelX + 15, y, panelX + PANEL_WIDTH - 15, y + 1, 0xFF666666);
        y += 12;
        context.drawCenteredTextWithShadow(textRenderer,
                "Break one block. Complete quests.", centerX, y, 0xFFCCCCCC);
        y += 14;
        context.drawCenteredTextWithShadow(textRenderer,
                "Advance through 25 phases to win!", centerX, y, 0xFFCCCCCC);
        y += 14;
        context.drawCenteredTextWithShadow(textRenderer,
                "Choose how you want to join:", centerX, y, 0xFF999999);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}
