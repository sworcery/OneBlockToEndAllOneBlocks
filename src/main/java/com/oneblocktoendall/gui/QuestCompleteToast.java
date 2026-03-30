package com.oneblocktoendall.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;

public class QuestCompleteToast implements Toast {

    private final String questName;
    private static final long DISPLAY_TIME = 3000L;
    private long startTime = -1;

    public QuestCompleteToast(String questName) {
        this.questName = questName;
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
        context.drawBorder(0, 0, getWidth(), getHeight(), 0xFF55FF55);

        context.drawText(textRenderer, "\u2714 Quest Complete!", 8, 7, 0xFF55FF55, true);
        context.drawText(textRenderer, questName, 8, 19, 0xFFFFFFFF, true);
    }
}
