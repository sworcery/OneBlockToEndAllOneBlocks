package com.oneblocktoendall.gui;

import com.oneblocktoendall.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class SettingsScreen extends Screen {

    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 180;
    private final Screen parent;

    public SettingsScreen(Screen parent) {
        super(Text.literal("Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        ModConfig config = ModConfig.get();

        int centerX = this.width / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        int btnWidth = 120;
        int btnX = centerX + 10;
        int y = panelY + 35;
        int spacing = 26;

        // HUD Enabled toggle
        addDrawableChild(ButtonWidget.builder(
                Text.literal(config.hudEnabled ? "ON" : "OFF"),
                button -> {
                    config.hudEnabled = !config.hudEnabled;
                    button.setMessage(Text.literal(config.hudEnabled ? "ON" : "OFF"));
                    ModConfig.save();
                }
        ).dimensions(btnX, y, btnWidth, 20).build());
        y += spacing;

        // HUD Position cycle
        addDrawableChild(ButtonWidget.builder(
                Text.literal(config.hudPosition.displayName()),
                button -> {
                    config.hudPosition = config.hudPosition.next();
                    button.setMessage(Text.literal(config.hudPosition.displayName()));
                    ModConfig.save();
                }
        ).dimensions(btnX, y, btnWidth, 20).build());
        y += spacing;

        // HUD Scale -/+ buttons
        addDrawableChild(ButtonWidget.builder(Text.literal("-"), button -> {
            config.hudScale = Math.max(0.5f, config.hudScale - 0.25f);
            ModConfig.save();
        }).dimensions(btnX, y, 30, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> {
            config.hudScale = Math.min(2.0f, config.hudScale + 0.25f);
            ModConfig.save();
        }).dimensions(btnX + btnWidth - 30, y, 30, 20).build());
        y += spacing;

        // Toasts toggle
        addDrawableChild(ButtonWidget.builder(
                Text.literal(config.toastsEnabled ? "ON" : "OFF"),
                button -> {
                    config.toastsEnabled = !config.toastsEnabled;
                    button.setMessage(Text.literal(config.toastsEnabled ? "ON" : "OFF"));
                    ModConfig.save();
                }
        ).dimensions(btnX, y, btnWidth, 20).build());

        // Done button
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
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

        // Title
        context.drawCenteredTextWithShadow(textRenderer, "Settings", centerX, panelY + 10, 0xFFFFD700);
        context.fill(panelX + 10, panelY + 24, panelX + PANEL_WIDTH - 10, panelY + 25, 0xFF666666);

        // Labels
        int labelX = panelX + 15;
        int y = panelY + 40;
        int spacing = 26;

        context.drawTextWithShadow(textRenderer, "HUD Enabled", labelX, y, 0xFFDDDDDD);
        y += spacing;
        context.drawTextWithShadow(textRenderer, "HUD Position", labelX, y, 0xFFDDDDDD);
        y += spacing;
        String scaleText = "HUD Scale: " + String.format("%.0f%%", ModConfig.get().hudScale * 100);
        context.drawTextWithShadow(textRenderer, scaleText, labelX, y, 0xFFDDDDDD);
        y += spacing;
        context.drawTextWithShadow(textRenderer, "Toast Popups", labelX, y, 0xFFDDDDDD);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
