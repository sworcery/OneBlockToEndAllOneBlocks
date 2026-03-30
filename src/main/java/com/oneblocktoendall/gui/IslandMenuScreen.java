package com.oneblocktoendall.gui;

import com.oneblocktoendall.network.IslandListPayload;
import com.oneblocktoendall.network.TeleportRequestPayload;
import com.oneblocktoendall.network.VisitorTogglePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class IslandMenuScreen extends Screen {

    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 260;
    private final IslandListPayload data;

    public IslandMenuScreen(IslandListPayload data) {
        super(Text.literal("Islands"));
        this.data = data;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        int y = panelY + 55;
        MinecraftClient mc = MinecraftClient.getInstance();
        String myName = mc.getSession().getUsername();

        for (IslandListPayload.IslandInfo island : data.islands()) {
            if (y > panelY + PANEL_HEIGHT - 50) break;

            if (!island.playerName().equals(myName)) {
                if (island.allowsVisitors() && island.online()) {
                    final IslandListPayload.IslandInfo target = island;
                    addDrawableChild(ButtonWidget.builder(Text.literal("Visit"), button -> {
                        ClientPlayNetworking.send(new TeleportRequestPayload(target.playerId()));
                        close();
                    }).dimensions(panelX + PANEL_WIDTH - 60, y - 2, 45, 16).build());
                }
            } else {
                // Visitor toggle for own island
                addDrawableChild(ButtonWidget.builder(
                        Text.literal(island.allowsVisitors() ? "Open" : "Closed"),
                        button -> {
                            boolean newVal = !island.allowsVisitors();
                            ClientPlayNetworking.send(new VisitorTogglePayload(newVal));
                            button.setMessage(Text.literal(newVal ? "Open" : "Closed"));
                        }
                ).dimensions(panelX + PANEL_WIDTH - 60, y - 2, 45, 16).build());
            }
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
        context.drawBorder(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFFFFD700);

        int y = panelY + 10;
        context.drawCenteredTextWithShadow(textRenderer, "Player Islands", centerX, y, 0xFFFFD700);
        y += 16;
        context.fill(panelX + 10, y, panelX + PANEL_WIDTH - 10, y + 1, 0xFF666666);
        y += 8;

        // Column headers
        context.drawTextWithShadow(textRenderer, "Player", panelX + 15, y, 0xFFFFD700);
        context.drawTextWithShadow(textRenderer, "Phase", panelX + 140, y, 0xFFFFD700);
        context.drawTextWithShadow(textRenderer, "Status", panelX + 190, y, 0xFFFFD700);
        y += 14;

        for (IslandListPayload.IslandInfo island : data.islands()) {
            if (y > panelY + PANEL_HEIGHT - 50) break;
            int color = island.online() ? 0xFFFFFFFF : 0xFF888888;
            context.drawTextWithShadow(textRenderer, island.playerName(), panelX + 15, y, color);
            context.drawTextWithShadow(textRenderer, "P" + island.phase(), panelX + 140, y, color);
            String status = island.online() ? (island.allowsVisitors() ? "Open" : "Closed") : "Offline";
            int statusColor = island.online() ? (island.allowsVisitors() ? 0xFF55FF55 : 0xFFFF5555) : 0xFF888888;
            context.drawTextWithShadow(textRenderer, status, panelX + 190, y, statusColor);
            y += 22;
        }

        if (data.islands().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer,
                    "No islands to display", centerX, panelY + 80, 0xFF888888);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}
