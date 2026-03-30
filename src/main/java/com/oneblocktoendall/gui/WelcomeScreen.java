package com.oneblocktoendall.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Welcome popup shown when the player joins a world.
 * Displays the mod name, basic instructions, controls, and key bindings
 * so the player knows how to interact with the one block challenge.
 */
public class WelcomeScreen extends Screen {

    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 220;

    public WelcomeScreen() {
        super(Text.literal("Welcome"));
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        // "Let's Go!" dismiss button at the bottom of the panel
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Let's Go!"),
                button -> close()
        ).dimensions(centerX - 50, panelY + PANEL_HEIGHT - 32, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark overlay without blur (1.21.4's renderBackground() adds unwanted blur)
        context.fill(0, 0, this.width, this.height, 0xA0000000);

        int centerX = this.width / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        // Panel background — solid dark blue, fully opaque
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT,
                0xFF1A1A2E);
        // Gold border
        context.drawBorder(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFFFFD700);

        int y = panelY + 12;
        int lineHeight = 13;

        // Title
        context.drawCenteredTextWithShadow(textRenderer,
                "One Block to Rule Them All", centerX, y, 0xFFFFD700);
        y += lineHeight + 4;

        // Divider
        context.fill(panelX + 15, y, panelX + PANEL_WIDTH - 15, y + 1, 0xFF666666);
        y += 8;

        // Instructions
        context.drawCenteredTextWithShadow(textRenderer,
                "Break the block. Complete quests.", centerX, y, 0xFFCCCCCC);
        y += lineHeight;
        context.drawCenteredTextWithShadow(textRenderer,
                "Advance through 25 phases to win!", centerX, y, 0xFFCCCCCC);
        y += lineHeight + 8;

        // Controls header
        context.drawCenteredTextWithShadow(textRenderer,
                "--- Controls ---", centerX, y, 0xFFFFD700);
        y += lineHeight + 4;

        // Key bindings
        drawKeyBinding(context, panelX + 20, y, "J", "Open Quest Book");
        y += lineHeight + 2;
        drawKeyBinding(context, panelX + 20, y, "HUD", "Quest progress (top-right)");
        y += lineHeight + 2;
        drawKeyBinding(context, panelX + 20, y, "/oneblock quests", "Show quests in chat");
        y += lineHeight + 2;
        drawKeyBinding(context, panelX + 20, y, "/oneblock phase", "Show current phase");
        y += lineHeight + 2;
        drawKeyBinding(context, panelX + 20, y, "/oneblock reset", "Reset and start over");
        y += lineHeight + 8;

        // Tips
        context.drawCenteredTextWithShadow(textRenderer,
                "Tip: The block respawns instantly!", centerX, y, 0xFF55FF55);

        super.render(context, mouseX, mouseY, delta);
    }

    /**
     * Draw a key binding line: [KEY] — Description
     */
    private void drawKeyBinding(DrawContext context, int x, int y,
                                 String key, String description) {
        // Key in gold brackets
        String keyText = "[" + key + "]";
        context.drawTextWithShadow(textRenderer, keyText, x, y, 0xFFFFAA00);

        // Description in white after the key
        int keyWidth = textRenderer.getWidth(keyText);
        context.drawTextWithShadow(textRenderer,
                " - " + description, x + keyWidth, y, 0xFFDDDDDD);
    }

    @Override
    public boolean shouldPause() {
        return false; // Don't pause the game
    }
}
