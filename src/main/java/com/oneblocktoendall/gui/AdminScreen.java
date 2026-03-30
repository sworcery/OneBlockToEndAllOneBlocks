package com.oneblocktoendall.gui;

import com.oneblocktoendall.network.AdminActionPayload;
import com.oneblocktoendall.network.AdminDataPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class AdminScreen extends Screen {

    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 280;
    private final AdminDataPayload data;

    public AdminScreen(AdminDataPayload data) {
        super(Text.literal("Admin Panel"));
        this.data = data;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        // Auto-start toggle
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Auto-Start: " + (data.autoStart() ? "ON" : "OFF")),
                button -> {
                    ClientPlayNetworking.send(new AdminActionPayload(
                            AdminActionPayload.TOGGLE_AUTO_START, "", 0));
                    close();
                }
        ).dimensions(panelX + 15, panelY + 35, 150, 16).build());

        // Per-player buttons
        int y = panelY + 70;
        for (AdminDataPayload.PlayerInfo player : data.players()) {
            if (y > panelY + PANEL_HEIGHT - 50) break;
            final String name = player.name();

            // Phase +/- buttons
            addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> {
                ClientPlayNetworking.send(new AdminActionPayload(
                        AdminActionPayload.SET_PHASE, name, player.phase() + 1));
                close();
            }).dimensions(panelX + PANEL_WIDTH - 105, y - 2, 20, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("-"), button -> {
                ClientPlayNetworking.send(new AdminActionPayload(
                        AdminActionPayload.SET_PHASE, name, Math.max(1, player.phase() - 1)));
                close();
            }).dimensions(panelX + PANEL_WIDTH - 80, y - 2, 20, 16).build());

            // Reset button
            addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), button -> {
                ClientPlayNetworking.send(new AdminActionPayload(
                        AdminActionPayload.RESET_PLAYER, name, 0));
                close();
            }).dimensions(panelX + PANEL_WIDTH - 55, y - 2, 40, 16).build());

            y += 22;
        }

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
        context.drawBorder(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFFFF5555);

        int y = panelY + 10;
        context.drawCenteredTextWithShadow(textRenderer, "Admin Panel", centerX, y, 0xFFFF5555);
        y += 16;
        context.fill(panelX + 10, y, panelX + PANEL_WIDTH - 10, y + 1, 0xFF666666);

        // Player list header
        y = panelY + 56;
        context.drawTextWithShadow(textRenderer, "Player", panelX + 15, y, 0xFFFFD700);
        context.drawTextWithShadow(textRenderer, "Phase", panelX + 130, y, 0xFFFFD700);
        context.drawTextWithShadow(textRenderer, "Status", panelX + 180, y, 0xFFFFD700);
        y += 14;

        for (AdminDataPayload.PlayerInfo player : data.players()) {
            if (y > panelY + PANEL_HEIGHT - 50) break;
            int color = player.online() ? 0xFFFFFFFF : 0xFF888888;
            context.drawTextWithShadow(textRenderer, player.name(), panelX + 15, y, color);
            context.drawTextWithShadow(textRenderer, "P" + player.phase(), panelX + 130, y, color);
            String status = player.spectating() ? "Spec" : (player.online() ? "Active" : "Offline");
            context.drawTextWithShadow(textRenderer, status, panelX + 180, y, color);
            y += 22;
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
