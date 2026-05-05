package com.oneblocktoendall.data;

import com.oneblocktoendall.OneBlockMod;
import com.oneblocktoendall.network.LeaderboardPayload;
import com.oneblocktoendall.quest.PlayerProgress;
import com.oneblocktoendall.team.Team;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.*;
import java.util.stream.Collectors;

public class OneBlockWorldState extends PersistentState {

    private final Map<UUID, PlayerProgress> playerData = new HashMap<>();
    private final Map<UUID, Team> teams = new HashMap<>();

    private static final Type<OneBlockWorldState> TYPE = new Type<>(
            OneBlockWorldState::new,
            OneBlockWorldState::fromNbt,
            null
    );

    public OneBlockWorldState() {}

    public PlayerProgress getOrCreateProgress(UUID playerId) {
        return playerData.computeIfAbsent(playerId, PlayerProgress::new);
    }

    public PlayerProgress getProgress(UUID playerId) {
        return playerData.get(playerId);
    }

    public Map<UUID, PlayerProgress> getAllProgress() {
        return playerData;
    }

    // --- Team Management ---
    public void addTeam(Team team) { teams.put(team.getTeamId(), team); }
    public Team getTeam(UUID teamId) { return teams.get(teamId); }
    public void removeTeam(UUID teamId) { teams.remove(teamId); }
    public Collection<Team> getAllTeams() { return teams.values(); }

    // --- Leaderboard ---
    public List<LeaderboardPayload.LeaderboardEntry> buildLeaderboard(MinecraftServer server) {
        return playerData.entrySet().stream()
                .filter(e -> e.getValue().isStarted())
                .sorted((a, b) -> {
                    int phaseCmp = Integer.compare(b.getValue().getCurrentPhase(), a.getValue().getCurrentPhase());
                    if (phaseCmp != 0) return phaseCmp;
                    int questCmp = Integer.compare(b.getValue().getTotalQuestsCompleted(), a.getValue().getTotalQuestsCompleted());
                    if (questCmp != 0) return questCmp;
                    return Integer.compare(b.getValue().getTotalBlocksBroken(), a.getValue().getTotalBlocksBroken());
                })
                .map(e -> {
                    String name = resolveName(server, e.getKey());
                    PlayerProgress p = e.getValue();
                    return new LeaderboardPayload.LeaderboardEntry(
                            name, p.getCurrentPhase(), p.getTotalQuestsCompleted(), p.getTotalBlocksBroken());
                })
                .collect(Collectors.toList());
    }

    private String resolveName(MinecraftServer server, UUID playerId) {
        return com.oneblocktoendall.util.PlayerNames.resolve(server, playerId);
    }

    public static OneBlockWorldState get(MinecraftServer server) {
        PersistentStateManager manager = server.getWorld(World.OVERWORLD)
                .getPersistentStateManager();
        return manager.getOrCreate(TYPE, OneBlockMod.MOD_ID);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound playersNbt = new NbtCompound();
        for (Map.Entry<UUID, PlayerProgress> entry : playerData.entrySet()) {
            playersNbt.put(entry.getKey().toString(), entry.getValue().toNbt());
        }
        nbt.put("players", playersNbt);

        NbtCompound teamsNbt = new NbtCompound();
        for (Map.Entry<UUID, Team> entry : teams.entrySet()) {
            teamsNbt.put(entry.getKey().toString(), entry.getValue().toNbt());
        }
        nbt.put("teams", teamsNbt);

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

        if (nbt.contains("teams")) {
            NbtCompound teamsNbt = nbt.getCompound("teams");
            for (String key : teamsNbt.getKeys()) {
                NbtCompound teamNbt = teamsNbt.getCompound(key);
                Team team = Team.fromNbt(teamNbt);
                state.teams.put(team.getTeamId(), team);
            }
        }

        return state;
    }
}
