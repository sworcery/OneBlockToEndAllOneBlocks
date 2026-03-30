package com.oneblocktoendall.config;

public enum HudPosition {
    TOP_RIGHT,
    TOP_LEFT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT;

    public int getX(int screenWidth, int panelWidth) {
        return switch (this) {
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - panelWidth - 10;
            case TOP_LEFT, BOTTOM_LEFT -> 10;
        };
    }

    public int getY(int screenHeight, int panelHeight) {
        return switch (this) {
            case TOP_RIGHT, TOP_LEFT -> 10;
            case BOTTOM_RIGHT, BOTTOM_LEFT -> screenHeight - panelHeight - 10;
        };
    }

    public HudPosition next() {
        HudPosition[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public String displayName() {
        return switch (this) {
            case TOP_RIGHT -> "Top Right";
            case TOP_LEFT -> "Top Left";
            case BOTTOM_RIGHT -> "Bottom Right";
            case BOTTOM_LEFT -> "Bottom Left";
        };
    }
}
