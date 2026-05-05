package com.oneblocktoendall.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class PlayerNames {

    public static String resolve(MinecraftServer server, UUID playerId) {
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(playerId);
        if (online != null) return online.getName().getString();
        return server.getUserCache().getByUuid(playerId)
                .map(profile -> profile.getName())
                .orElse("Unknown");
    }
}
