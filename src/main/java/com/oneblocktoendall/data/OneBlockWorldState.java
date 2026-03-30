package com.oneblocktoendall.data;

import com.oneblocktoendall.OneBlockMod;
import com.oneblocktoendall.quest.PlayerProgress;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent world state that saves all player progress data.
 * Attached to the overworld so it persists across server restarts.
 *
 * Minecraft's PersistentState system handles saving/loading automatically
 * as part of world saves.
 */
public class OneBlockWorldState extends PersistentState {

    private final Map<UUID, PlayerProgress> playerData = new HashMap<>();

    private static final Type<OneBlockWorldState> TYPE = new Type<>(
            OneBlockWorldState::new,
            OneBlockWorldState::fromNbt,
            null  // No DataFixTypes needed for mod data
    );

    public OneBlockWorldState() {
    }

    /**
     * Get or create progress data for a player.
     */
    public PlayerProgress getOrCreateProgress(UUID playerId) {
        return playerData.computeIfAbsent(playerId, PlayerProgress::new);
    }

    /**
     * Get progress for a player, or null if they haven't started.
     */
    public PlayerProgress getProgress(UUID playerId) {
        return playerData.get(playerId);
    }

    /**
     * Get all player progress entries (for tick handler iteration).
     */
    public Map<UUID, PlayerProgress> getAllProgress() {
        return playerData;
    }

    /**
     * Retrieve the world state instance from the server.
     * Creates it if it doesn't exist yet.
     */
    public static OneBlockWorldState get(MinecraftServer server) {
        PersistentStateManager manager = server.getWorld(World.OVERWORLD)
                .getPersistentStateManager();
        return manager.getOrCreate(TYPE, OneBlockMod.MOD_ID);
    }

    // --- Serialization ---

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound playersNbt = new NbtCompound();
        for (Map.Entry<UUID, PlayerProgress> entry : playerData.entrySet()) {
            playersNbt.put(entry.getKey().toString(), entry.getValue().toNbt());
        }
        nbt.put("players", playersNbt);
        return nbt;
    }

    public static OneBlockWorldState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        OneBlockWorldState state = new OneBlockWorldState();
        NbtCompound playersNbt = nbt.getCompound("players");
        for (String key : playersNbt.getKeys()) {
            NbtCompound playerNbt = playersNbt.getCompound(key);
            PlayerProgress progress = PlayerProgress.fromNbt(playerNbt);
            state.playerData.put(progress.getPlayerId(), progress);
        }
        return state;
    }
}
