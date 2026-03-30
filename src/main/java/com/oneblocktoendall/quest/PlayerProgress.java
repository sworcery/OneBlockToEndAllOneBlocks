package com.oneblocktoendall.quest;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * Tracks one player's progress through the one block challenge.
 * Serializable to/from NBT for world save persistence.
 *
 * Key concept: "baselines" — when a phase starts, we record the player's current
 * Minecraft stats. Quest progress = current stat - baseline. This way, quests
 * track progress from when the phase began, not lifetime totals.
 */
public class PlayerProgress {

    private UUID playerId;
    private boolean started = false;
    private int currentPhase = 0;  // 0 = not started, 1-10 = active phase
    private BlockPos oneBlockPos = BlockPos.ORIGIN;
    private int totalBlocksBroken = 0;  // Lifetime block break counter

    /** Baseline stat values recorded when each quest became active. questId -> baseline */
    private final Map<String, Integer> questBaselines = new HashMap<>();

    /** Set of completed quest IDs. */
    private final Set<String> completedQuests = new HashSet<>();

    /** Whether the player has been notified of phase completion (prevents spam). */
    private boolean phaseCompleteNotified = false;

    public PlayerProgress(UUID playerId) {
        this.playerId = playerId;
    }

    // --- Getters / Setters ---

    public UUID getPlayerId() { return playerId; }
    public boolean isStarted() { return started; }
    public int getCurrentPhase() { return currentPhase; }
    public BlockPos getOneBlockPos() { return oneBlockPos; }
    public boolean isPhaseCompleteNotified() { return phaseCompleteNotified; }

    public int getTotalBlocksBroken() { return totalBlocksBroken; }

    public void setStarted(boolean started) { this.started = started; }
    public void setOneBlockPos(BlockPos pos) { this.oneBlockPos = pos; }
    public void setPhaseCompleteNotified(boolean notified) { this.phaseCompleteNotified = notified; }

    public void setCurrentPhase(int phase) {
        this.currentPhase = phase;
        this.phaseCompleteNotified = false;
    }

    /** Increment the block break counter and return the new total. */
    public int incrementBlocksBroken() {
        return ++totalBlocksBroken;
    }

    // --- Quest Baselines ---

    public void setBaseline(String questId, int value) {
        questBaselines.put(questId, value);
    }

    public int getBaseline(String questId) {
        return questBaselines.getOrDefault(questId, 0);
    }

    public boolean hasBaseline(String questId) {
        return questBaselines.containsKey(questId);
    }

    // --- Quest Completion ---

    public void completeQuest(String questId) {
        completedQuests.add(questId);
    }

    public boolean isQuestCompleted(String questId) {
        return completedQuests.contains(questId);
    }

    public Set<String> getCompletedQuests() {
        return Collections.unmodifiableSet(completedQuests);
    }

    /**
     * Reset all progress data for a fresh start.
     * Called by /oneblock reset.
     */
    public void resetAll() {
        this.started = false;
        this.currentPhase = 0;
        this.oneBlockPos = BlockPos.ORIGIN;
        this.phaseCompleteNotified = false;
        this.totalBlocksBroken = 0;
        this.questBaselines.clear();
        this.completedQuests.clear();
    }

    // --- NBT Serialization ---

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("playerId", playerId);
        nbt.putBoolean("started", started);
        nbt.putInt("currentPhase", currentPhase);
        nbt.putInt("blockX", oneBlockPos.getX());
        nbt.putInt("blockY", oneBlockPos.getY());
        nbt.putInt("blockZ", oneBlockPos.getZ());
        nbt.putBoolean("phaseCompleteNotified", phaseCompleteNotified);
        nbt.putInt("totalBlocksBroken", totalBlocksBroken);

        // Baselines
        NbtCompound baselinesNbt = new NbtCompound();
        for (Map.Entry<String, Integer> entry : questBaselines.entrySet()) {
            baselinesNbt.putInt(entry.getKey(), entry.getValue());
        }
        nbt.put("baselines", baselinesNbt);

        // Completed quests stored as comma-separated string
        nbt.putString("completedQuests", String.join(",", completedQuests));

        return nbt;
    }

    public static PlayerProgress fromNbt(NbtCompound nbt) {
        UUID id = nbt.getUuid("playerId");
        PlayerProgress progress = new PlayerProgress(id);
        progress.started = nbt.getBoolean("started");
        progress.currentPhase = nbt.getInt("currentPhase");
        progress.oneBlockPos = new BlockPos(
                nbt.getInt("blockX"), nbt.getInt("blockY"), nbt.getInt("blockZ"));
        progress.phaseCompleteNotified = nbt.getBoolean("phaseCompleteNotified");
        progress.totalBlocksBroken = nbt.getInt("totalBlocksBroken");

        // Baselines
        NbtCompound baselinesNbt = nbt.getCompound("baselines");
        for (String key : baselinesNbt.getKeys()) {
            progress.questBaselines.put(key, baselinesNbt.getInt(key));
        }

        // Completed quests
        String completed = nbt.getString("completedQuests");
        if (!completed.isEmpty()) {
            progress.completedQuests.addAll(Arrays.asList(completed.split(",")));
        }

        return progress;
    }
}
