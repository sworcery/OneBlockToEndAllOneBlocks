package com.oneblocktoendall.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;

public class PhaseCompleteToast implements Toast {

    private final String phaseName;
    private final int phaseNumber;
    private final String playerName;
    private final boolean isBroadcast;
    private static final long DISPLAY_TIME = 5000L;
    private long startTime = -1;

    public PhaseCompleteToast(String phaseName, int phaseNumber, String playerName, boolean isBroadcast) {
        this.phaseName = phaseName;
        this.phaseNumber = phaseNumber;
        this.playerName = playerName;
        this.isBroadcast = isBroadcast;
    }

    @Override
    public Visibility getVisibility() {
        if (startTime < 0) return Visibility.SHOW;
        return System.currentTimeMillis() - startTime >= DISPLAY_TIME ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public void update(ToastManager manager, long time) {
        if (startTime < 0) startTime = System.currentTimeMillis();
    }

    @Override
    public void draw(DrawContext context, TextRenderer textRenderer, long startTime) {
        context.fill(0, 0, getWidth(), getHeight(), 0xFF1A1A2E);
        context.drawBorder(0, 0, getWidth(), getHeight(), 0xFFFFD700);

        if (isBroadcast) {
            context.drawText(textRenderer, playerName + " reached", 8, 7, 0xFFFFD700, true);
            context.drawText(textRenderer, "Phase " + phaseNumber + ": " + phaseName, 8, 19, 0xFFFFFFFF, true);
        } else {
            context.drawText(textRenderer, "Phase Complete!", 8, 7, 0xFFFFD700, true);
            context.drawText(textRenderer, "Phase " + phaseNumber + ": " + phaseName, 8, 19, 0xFFFFFFFF, true);
        }
    }
}
