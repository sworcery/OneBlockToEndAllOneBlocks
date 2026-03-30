package com.oneblocktoendall.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.oneblocktoendall.OneBlockMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerConfig {

    private static ServerConfig INSTANCE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "config/oneblocktoendall-server.json";

    public boolean autoStart = false;

    public static ServerConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static void load() {
        Path path = Path.of(FILE_NAME);
        if (Files.exists(path)) {
            try {
                String json = Files.readString(path);
                INSTANCE = GSON.fromJson(json, ServerConfig.class);
                if (INSTANCE == null) INSTANCE = new ServerConfig();
            } catch (Exception e) {
                OneBlockMod.LOGGER.warn("Failed to load server config, using defaults", e);
                INSTANCE = new ServerConfig();
            }
        } else {
            INSTANCE = new ServerConfig();
            save();
        }
    }

    public static void save() {
        try {
            Path path = Path.of(FILE_NAME);
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(get()));
        } catch (IOException e) {
            OneBlockMod.LOGGER.warn("Failed to save server config", e);
        }
    }
}
