package com.oneblocktoendall.gui;

import com.oneblocktoendall.network.BlockPoolPayload;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class BlockPoolScreen extends Screen {

    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 240;
    private final BlockPoolPayload data;
    private int scrollOffset = 0;

    public BlockPoolScreen(BlockPoolPayload data) {
        super(Text.literal("Block Pool"));
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
        context.drawCenteredTextWithShadow(textRenderer,
                "Phase " + data.phase() + " Drop Pool", centerX, y, 0xFFFFD700);
        y += 16;
        context.fill(panelX + 10, y, panelX + PANEL_WIDTH - 10, y + 1, 0xFF666666);
        y += 6;

        // Calculate total weight for percentages
        int totalBlockWeight = data.blocks().stream().mapToInt(BlockPoolPayload.PoolEntry::weight).sum();

        // Block drops
        context.drawTextWithShadow(textRenderer, "Block Drops:", panelX + 12, y, 0xFF55FF55);
        y += 13;

        int maxY = panelY + PANEL_HEIGHT - 70;
        int displayed = 0;
        for (int i = scrollOffset; i < data.blocks().size() && y < maxY; i++) {
            BlockPoolPayload.PoolEntry entry = data.blocks().get(i);
            String name = entry.id().replace("minecraft:", "").replace("_", " ");
            float pct = totalBlockWeight > 0 ? (entry.weight() * 100f / totalBlockWeight) : 0;
            String line = String.format("  %-20s %5.1f%%", name, pct);
            context.drawTextWithShadow(textRenderer, line, panelX + 15, y, 0xFFCCCCCC);
            y += 11;
            displayed++;
        }

        if (data.blocks().size() > displayed + scrollOffset) {
            context.drawCenteredTextWithShadow(textRenderer,
                    "... scroll for more ...", centerX, y, 0xFF888888);
            y += 13;
        }

        y += 4;

        // Mob spawns
        if (!data.mobs().isEmpty()) {
            context.drawTextWithShadow(textRenderer,
                    "Mob Spawns (" + (int)(data.mobSpawnChance() * 100) + "% chance):",
                    panelX + 12, y, 0xFFFF8844);
            y += 13;
            int totalMobWeight = data.mobs().stream().mapToInt(BlockPoolPayload.PoolEntry::weight).sum();
            for (BlockPoolPayload.PoolEntry entry : data.mobs()) {
                if (y >= maxY) break;
                String name = entry.id().replace("minecraft:", "").replace("_", " ");
                float pct = totalMobWeight > 0 ? (entry.weight() * 100f / totalMobWeight) : 0;
                context.drawTextWithShadow(textRenderer,
                        String.format("  %-20s %5.1f%%", name, pct), panelX + 15, y, 0xFFCCCCCC);
                y += 11;
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int) verticalAmount, data.blocks().size() - 1));
        return true;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Skip default blur — we draw our own dark overlay in render()
    }

    @Override
    public boolean shouldPause() { return false; }
}
