package com.oneblocktoendall.quest;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class PlayerProgress {

    private UUID playerId;
    private boolean started = false;
    private int currentPhase = 0;
    private BlockPos oneBlockPos = BlockPos.ORIGIN;
    private int totalBlocksBroken = 0;

    private final Map<String, Integer> questBaselines = new HashMap<>();
    private final Set<String> completedQuests = new HashSet<>();
    private boolean phaseCompleteNotified = false;

    // New fields for features
    private boolean spectating = false;
    private boolean allowVisitors = true;
    private long challengeStartTime = 0;
    private int totalQuestsCompleted = 0;
    private UUID teamId = null;

    public PlayerProgress(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() { return playerId; }
    public boolean isStarted() { return started; }
    public int getCurrentPhase() { return currentPhase; }
    public BlockPos getOneBlockPos() { return oneBlockPos; }
    public boolean isPhaseCompleteNotified() { return phaseCompleteNotified; }
    public int getTotalBlocksBroken() { return totalBlocksBroken; }
    public boolean isSpectating() { return spectating; }
    public boolean isAllowVisitors() { return allowVisitors; }
    public long getChallengeStartTime() { return challengeStartTime; }
    public int getTotalQuestsCompleted() { return totalQuestsCompleted; }
    public UUID getTeamId() { return teamId; }

    public void setStarted(boolean started) { this.started = started; }
    public void setOneBlockPos(BlockPos pos) { this.oneBlockPos = pos; }
    public void setPhaseCompleteNotified(boolean notified) { this.phaseCompleteNotified = notified; }
    public void setSpectating(boolean spectating) { this.spectating = spectating; }
    public void setAllowVisitors(boolean allowVisitors) { this.allowVisitors = allowVisitors; }
    public void setChallengeStartTime(long time) { this.challengeStartTime = time; }
    public void setTeamId(UUID teamId) { this.teamId = teamId; }

    public void setCurrentPhase(int phase) {
        this.currentPhase = phase;
        this.phaseCompleteNotified = false;
        this.questBaselines.clear();
    }

    public int incrementBlocksBroken() {
        return ++totalBlocksBroken;
    }

    // --- Quest Baselines ---
    public void setBaseline(String questId, int value) { questBaselines.put(questId, value); }
    public int getBaseline(String questId) { return questBaselines.getOrDefault(questId, 0); }
    public boolean hasBaseline(String questId) { return questBaselines.containsKey(questId); }

    // --- Quest Completion ---
    public void completeQuest(String questId) {
        completedQuests.add(questId);
        totalQuestsCompleted++;
    }

    public boolean isQuestCompleted(String questId) { return completedQuests.contains(questId); }
    public Set<String> getCompletedQuests() { return Collections.unmodifiableSet(completedQuests); }

    public void resetAll() {
        this.started = false;
        this.currentPhase = 0;
        this.oneBlockPos = BlockPos.ORIGIN;
        this.phaseCompleteNotified = false;
        this.totalBlocksBroken = 0;
        this.questBaselines.clear();
        this.completedQuests.clear();
        this.spectating = false;
        this.challengeStartTime = 0;
        this.totalQuestsCompleted = 0;
        // Don't reset allowVisitors or teamId on challenge reset
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
        nbt.putBoolean("spectating", spectating);
        nbt.putBoolean("allowVisitors", allowVisitors);
        nbt.putLong("challengeStartTime", challengeStartTime);
        nbt.putInt("totalQuestsCompleted", totalQuestsCompleted);
        if (teamId != null) nbt.putUuid("teamId", teamId);

        NbtCompound baselinesNbt = new NbtCompound();
        for (Map.Entry<String, Integer> entry : questBaselines.entrySet()) {
            baselinesNbt.putInt(entry.getKey(), entry.getValue());
        }
        nbt.put("baselines", baselinesNbt);

        NbtList completedList = new NbtList();
        for (String id : completedQuests) completedList.add(NbtString.of(id));
        nbt.put("completedQuests", completedList);

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

        // New fields with backwards compatibility
        if (nbt.contains("spectating")) progress.spectating = nbt.getBoolean("spectating");
        if (nbt.contains("allowVisitors")) progress.allowVisitors = nbt.getBoolean("allowVisitors");
        else progress.allowVisitors = true;
        if (nbt.contains("challengeStartTime")) progress.challengeStartTime = nbt.getLong("challengeStartTime");
        if (nbt.contains("totalQuestsCompleted")) progress.totalQuestsCompleted = nbt.getInt("totalQuestsCompleted");
        if (nbt.contains("teamId")) progress.teamId = nbt.getUuid("teamId");

        NbtCompound baselinesNbt = nbt.getCompound("baselines");
        for (String key : baselinesNbt.getKeys()) {
            progress.questBaselines.put(key, baselinesNbt.getInt(key));
        }

        // Support both legacy comma-string format and current NbtList format
        if (nbt.contains("completedQuests", 9)) { // 9 = NbtList
            NbtList completedList = nbt.getList("completedQuests", 8); // 8 = NbtString
            for (int i = 0; i < completedList.size(); i++) {
                progress.completedQuests.add(completedList.getString(i));
            }
        } else {
            String completed = nbt.getString("completedQuests");
            if (!completed.isEmpty()) {
                progress.completedQuests.addAll(Arrays.asList(completed.split(",")));
            }
        }

        return progress;
    }
}
