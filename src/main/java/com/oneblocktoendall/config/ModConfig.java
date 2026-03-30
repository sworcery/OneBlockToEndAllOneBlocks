package com.oneblocktoendall.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.oneblocktoendall.OneBlockMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {

    private static ModConfig INSTANCE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "oneblocktoendall.json";

    public boolean hudEnabled = true;
    public HudPosition hudPosition = HudPosition.TOP_RIGHT;
    public float hudScale = 1.0f;
    public boolean toastsEnabled = true;

    public static ModConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static void load() {
        Path path = getPath();
        if (Files.exists(path)) {
            try {
                String json = Files.readString(path);
                INSTANCE = GSON.fromJson(json, ModConfig.class);
                if (INSTANCE == null) INSTANCE = new ModConfig();
            } catch (Exception e) {
                OneBlockMod.LOGGER.warn("Failed to load config, using defaults", e);
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
            save();
        }
    }

    public static void save() {
        try {
            Path path = getPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(get()));
        } catch (IOException e) {
            OneBlockMod.LOGGER.warn("Failed to save config", e);
        }
    }

    private static Path getPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }
}
