package com.oneblocktoendall.gui;

import com.oneblocktoendall.network.TeamActionPayload;
import com.oneblocktoendall.network.TeamDataPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class TeamScreen extends Screen {

    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 260;
    private final TeamDataPayload data;
    private TextFieldWidget nameField;

    public TeamScreen(TeamDataPayload data) {
        super(Text.literal("Team"));
        this.data = data;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        if (!data.hasTeam()) {
            // Create team UI
            nameField = new TextFieldWidget(textRenderer, centerX - 70, panelY + 60, 140, 18,
                    Text.literal("Team Name"));
            nameField.setMaxLength(20);
            addDrawableChild(nameField);

            addDrawableChild(ButtonWidget.builder(Text.literal("Create Team"), button -> {
                String name = nameField.getText().trim();
                if (!name.isEmpty()) {
                    ClientPlayNetworking.send(new TeamActionPayload(TeamActionPayload.CREATE, name));
                    close();
                }
            }).dimensions(centerX - 60, panelY + 90, 120, 20).build());

            // Show pending invites
            int y = panelY + 125;
            if (!data.pendingInvites().isEmpty()) {
                for (String invite : data.pendingInvites()) {
                    if (y > panelY + PANEL_HEIGHT - 50) break;
                    addDrawableChild(ButtonWidget.builder(Text.literal("Accept"), button -> {
                        ClientPlayNetworking.send(new TeamActionPayload(TeamActionPayload.ACCEPT, invite));
                        close();
                    }).dimensions(panelX + PANEL_WIDTH - 110, y, 45, 16).build());
                    addDrawableChild(ButtonWidget.builder(Text.literal("Deny"), button -> {
                        ClientPlayNetworking.send(new TeamActionPayload(TeamActionPayload.DECLINE, invite));
                        close();
                    }).dimensions(panelX + PANEL_WIDTH - 60, y, 45, 16).build());
                    y += 22;
                }
            }
        } else {
            // Team management UI
            boolean isLeader = data.leaderName().equals(
                    net.minecraft.client.MinecraftClient.getInstance().getSession().getUsername());

            if (isLeader) {
                // Invite field
                nameField = new TextFieldWidget(textRenderer, panelX + 15, panelY + PANEL_HEIGHT - 75, 140, 18,
                        Text.literal("Player Name"));
                nameField.setMaxLength(16);
                addDrawableChild(nameField);

                addDrawableChild(ButtonWidget.builder(Text.literal("Invite"), button -> {
                    String name = nameField.getText().trim();
                    if (!name.isEmpty()) {
                        ClientPlayNetworking.send(new TeamActionPayload(TeamActionPayload.INVITE, name));
                        close();
                    }
                }).dimensions(panelX + 165, panelY + PANEL_HEIGHT - 75, 60, 18).build());

                // Kick buttons next to members
                int y = panelY + 80;
                for (String member : data.members()) {
                    if (member.equals(data.leaderName())) { y += 16; continue; }
                    if (y > panelY + PANEL_HEIGHT - 100) break;
                    final String m = member;
                    addDrawableChild(ButtonWidget.builder(Text.literal("Kick"), button -> {
                        ClientPlayNetworking.send(new TeamActionPayload(TeamActionPayload.KICK, m));
                        close();
                    }).dimensions(panelX + PANEL_WIDTH - 55, y - 2, 40, 14).build());
                    y += 16;
                }
            }

            // Leave button (always available)
            addDrawableChild(ButtonWidget.builder(Text.literal("Leave Team"), button -> {
                ClientPlayNetworking.send(new TeamActionPayload(TeamActionPayload.LEAVE, ""));
                close();
            }).dimensions(centerX - 50, panelY + PANEL_HEIGHT - 50, 100, 16).build());
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

        if (!data.hasTeam()) {
            context.drawCenteredTextWithShadow(textRenderer, "Team", centerX, y, 0xFFFFD700);
            y += 16;
            context.fill(panelX + 10, y, panelX + PANEL_WIDTH - 10, y + 1, 0xFF666666);
            y += 10;
            context.drawCenteredTextWithShadow(textRenderer,
                    "You're not in a team yet.", centerX, y, 0xFFCCCCCC);
            y += 20;
            context.drawCenteredTextWithShadow(textRenderer,
                    "Create one or accept an invite:", centerX, y, 0xFF999999);

            // Show pending invites labels
            y = panelY + 125;
            if (!data.pendingInvites().isEmpty()) {
                for (String invite : data.pendingInvites()) {
                    if (y > panelY + PANEL_HEIGHT - 50) break;
                    context.drawTextWithShadow(textRenderer, "Invite from: " + invite, panelX + 15, y + 3, 0xFFFFFF55);
                    y += 22;
                }
            }
        } else {
            context.drawCenteredTextWithShadow(textRenderer,
                    "Team: " + data.teamName(), centerX, y, 0xFFFFD700);
            y += 16;
            context.fill(panelX + 10, y, panelX + PANEL_WIDTH - 10, y + 1, 0xFF666666);
            y += 8;

            context.drawTextWithShadow(textRenderer, "Leader: " + data.leaderName(),
                    panelX + 15, y, 0xFFFFAA00);
            y += 16;

            if (data.mergedIslands()) {
                context.drawTextWithShadow(textRenderer, "Islands: Merged",
                        panelX + 15, y, 0xFF55FF55);
                y += 14;
            }

            context.drawTextWithShadow(textRenderer, "Members:", panelX + 15, y, 0xFFCCCCCC);
            y += 14;
            for (String member : data.members()) {
                if (y > panelY + PANEL_HEIGHT - 100) break;
                String prefix = member.equals(data.leaderName()) ? "\u2605 " : "  \u2022 ";
                context.drawTextWithShadow(textRenderer, prefix + member, panelX + 20, y, 0xFFFFFFFF);
                y += 16;
            }

            // Show message if present
            if (!data.message().isEmpty()) {
                context.drawCenteredTextWithShadow(textRenderer, data.message(),
                        centerX, panelY + PANEL_HEIGHT - 95, 0xFF55FF55);
            }
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
