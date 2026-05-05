package com.oneblocktoendall.network;

import com.oneblocktoendall.command.OneBlockCommand;
import com.oneblocktoendall.config.ServerConfig;
import com.oneblocktoendall.data.OneBlockWorldState;
import com.oneblocktoendall.phase.BlockPoolEntry;
import com.oneblocktoendall.phase.MobSpawnEntry;
import com.oneblocktoendall.phase.Phase;
import com.oneblocktoendall.phase.PhaseManager;
import com.oneblocktoendall.quest.PlayerProgress;
import com.oneblocktoendall.quest.Quest;
import com.oneblocktoendall.quest.QuestManager;
import com.oneblocktoendall.team.Team;
import com.oneblocktoendall.team.TeamManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class ModNetworking {

    private static final Map<UUID, Long> lastRequestTime = new HashMap<>();
    private static final long REQUEST_COOLDOWN_MS = 500;

    private static boolean isRateLimited(ServerPlayerEntity player) {
        long now = System.currentTimeMillis();
        Long last = lastRequestTime.get(player.getUuid());
        if (last != null && now - last < REQUEST_COOLDOWN_MS) return true;
        lastRequestTime.put(player.getUuid(), now);
        return false;
    }

    public static void registerS2CPackets() {
        PayloadTypeRegistry.playS2C().register(QuestSyncPayload.ID, QuestSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ToastPayload.ID, ToastPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PhaseAdvancePayload.ID, PhaseAdvancePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BlockPoolPayload.ID, BlockPoolPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StatsPayload.ID, StatsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LeaderboardPayload.ID, LeaderboardPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(IslandListPayload.ID, IslandListPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TeleportResponsePayload.ID, TeleportResponsePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AdminDataPayload.ID, AdminDataPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TeamDataPayload.ID, TeamDataPayload.CODEC);
    }

    public static void registerC2SPackets() {
        PayloadTypeRegistry.playC2S().register(StartChoicePayload.ID, StartChoicePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BlockPoolRequestPayload.ID, BlockPoolRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StatsRequestPayload.ID, StatsRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(LeaderboardRequestPayload.ID, LeaderboardRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(IslandListRequestPayload.ID, IslandListRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TeleportRequestPayload.ID, TeleportRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(VisitorTogglePayload.ID, VisitorTogglePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AdminRequestPayload.ID, AdminRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AdminActionPayload.ID, AdminActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TeamActionPayload.ID, TeamActionPayload.CODEC);

        // --- C2S Handlers ---

        ServerPlayNetworking.registerGlobalReceiver(StartChoicePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                if (payload.startChallenge()) {
                    OneBlockCommand.initializeChallenge(player);
                } else {
                    OneBlockWorldState state = OneBlockWorldState.get(player.server);
                    PlayerProgress progress = state.getOrCreateProgress(player.getUuid());
                    progress.setSpectating(true);
                    state.markDirty();
                    player.sendMessage(Text.literal("Spectating mode. Use /oneblock start to begin anytime.")
                            .formatted(Formatting.GRAY));
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(BlockPoolRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                if (isRateLimited(player)) return;
                OneBlockWorldState state = OneBlockWorldState.get(player.server);
                PlayerProgress progress = state.getProgress(player.getUuid());
                if (progress == null) return;
                int phase = Math.min(payload.requestedPhase(), progress.getCurrentPhase());
                if (phase < 1) return;
                sendBlockPoolData(player, phase);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(StatsRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                if (isRateLimited(player)) return;
                sendStatsData(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(LeaderboardRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                if (isRateLimited(player)) return;
                sendLeaderboard(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(IslandListRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                if (isRateLimited(player)) return;
                sendIslandList(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(TeleportRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> handleTeleport(player, payload.targetPlayerId()));
        });

        ServerPlayNetworking.registerGlobalReceiver(VisitorTogglePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                OneBlockWorldState state = OneBlockWorldState.get(player.server);
                PlayerProgress progress = state.getProgress(player.getUuid());
                if (progress != null) {
                    progress.setAllowVisitors(payload.allowVisitors());
                    state.markDirty();
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(AdminRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                if (!player.hasPermissionLevel(2)) return;
                sendAdminData(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(AdminActionPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> handleAdminAction(player, payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(TeamActionPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> handleTeamAction(player, payload));
        });
    }

    public static void syncQuestProgress(ServerPlayerEntity player, PlayerProgress progress) {
        Phase phase = PhaseManager.getPhase(progress.getCurrentPhase());
        if (phase == null) return;

        List<QuestSyncPayload.QuestStatus> questStatuses = new ArrayList<>();
        for (Quest quest : phase.quests()) {
            questStatuses.add(new QuestSyncPayload.QuestStatus(
                    quest.id(), quest.name(), quest.description(),
                    QuestManager.getQuestProgress(player, quest, progress),
                    quest.count(), progress.isQuestCompleted(quest.id())));
        }

        ServerPlayNetworking.send(player, new QuestSyncPayload(
                progress.getCurrentPhase(), PhaseManager.getMaxPhase(),
                phase.name(), questStatuses));
    }

    public static void sendToast(ServerPlayerEntity player, ToastPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void broadcastToast(MinecraftServer server, ToastPayload payload) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static void sendBlockPoolData(ServerPlayerEntity player, int phase) {
        List<BlockPoolPayload.PoolEntry> blocks = new ArrayList<>();
        List<BlockPoolPayload.PoolEntry> mobs = new ArrayList<>();
        double highestChance = 0;

        for (int i = 1; i <= phase; i++) {
            Phase p = PhaseManager.getPhase(i);
            if (p == null) continue;
            for (BlockPoolEntry e : p.blockPool()) {
                blocks.add(new BlockPoolPayload.PoolEntry(e.itemId(), e.weight()));
            }
            for (MobSpawnEntry e : p.mobSpawns()) {
                mobs.add(new BlockPoolPayload.PoolEntry(e.entityId(), e.weight()));
            }
            highestChance = Math.max(highestChance, p.mobSpawnChance());
        }

        ServerPlayNetworking.send(player, new BlockPoolPayload(phase, blocks, mobs, highestChance));
    }

    private static void sendStatsData(ServerPlayerEntity player) {
        OneBlockWorldState state = OneBlockWorldState.get(player.server);
        PlayerProgress progress = state.getProgress(player.getUuid());
        if (progress == null) return;

        List<StatsPayload.PhaseStats> phaseStats = new ArrayList<>();
        for (int i = 1; i <= progress.getCurrentPhase(); i++) {
            Phase phase = PhaseManager.getPhase(i);
            if (phase == null) continue;
            int done = (int) phase.quests().stream()
                    .filter(q -> progress.isQuestCompleted(q.id())).count();
            phaseStats.add(new StatsPayload.PhaseStats(i, phase.name(), done, phase.quests().size()));
        }

        ServerPlayNetworking.send(player, new StatsPayload(
                progress.getTotalBlocksBroken(),
                progress.getTotalQuestsCompleted(),
                progress.getCurrentPhase(),
                PhaseManager.getMaxPhase(),
                progress.getChallengeStartTime(),
                player.getServerWorld().getTime(),
                phaseStats));
    }

    private static void sendLeaderboard(ServerPlayerEntity player) {
        OneBlockWorldState state = OneBlockWorldState.get(player.server);
        List<LeaderboardPayload.LeaderboardEntry> entries = state.buildLeaderboard(player.server);
        ServerPlayNetworking.send(player, new LeaderboardPayload(entries, player.getName().getString()));
    }

    private static void sendIslandList(ServerPlayerEntity player) {
        OneBlockWorldState state = OneBlockWorldState.get(player.server);
        List<IslandListPayload.IslandInfo> islands = new ArrayList<>();

        for (Map.Entry<UUID, PlayerProgress> entry : state.getAllProgress().entrySet()) {
            PlayerProgress p = entry.getValue();
            if (!p.isStarted()) continue;
            String name = resolveName(player.server, entry.getKey());
            boolean online = player.server.getPlayerManager().getPlayer(entry.getKey()) != null;
            islands.add(new IslandListPayload.IslandInfo(
                    entry.getKey(), name, p.getCurrentPhase(), p.isAllowVisitors(), online));
        }

        ServerPlayNetworking.send(player, new IslandListPayload(islands));
    }

    private static void handleTeleport(ServerPlayerEntity player, UUID targetId) {
        OneBlockWorldState state = OneBlockWorldState.get(player.server);
        PlayerProgress targetProgress = state.getProgress(targetId);

        if (targetProgress == null || !targetProgress.isStarted()) {
            ServerPlayNetworking.send(player, new TeleportResponsePayload(false, "Island not found!"));
            return;
        }
        if (!targetProgress.isAllowVisitors()) {
            ServerPlayNetworking.send(player, new TeleportResponsePayload(false, "Visitors not allowed!"));
            return;
        }

        BlockPos pos = targetProgress.getOneBlockPos();
        player.teleport(player.getServerWorld(),
                pos.getX() + 2.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                java.util.Set.of(), player.getYaw(), player.getPitch(), false);

        String targetName = resolveName(player.server, targetId);
        ServerPlayNetworking.send(player, new TeleportResponsePayload(true, "Teleported to " + targetName + "'s island!"));
        player.sendMessage(Text.literal("Teleported to " + targetName + "'s island!").formatted(Formatting.GREEN));
    }

    private static void sendAdminData(ServerPlayerEntity admin) {
        OneBlockWorldState state = OneBlockWorldState.get(admin.server);
        List<AdminDataPayload.PlayerInfo> players = new ArrayList<>();

        for (Map.Entry<UUID, PlayerProgress> entry : state.getAllProgress().entrySet()) {
            PlayerProgress p = entry.getValue();
            String name = resolveName(admin.server, entry.getKey());
            boolean online = admin.server.getPlayerManager().getPlayer(entry.getKey()) != null;
            players.add(new AdminDataPayload.PlayerInfo(
                    name, p.getCurrentPhase(), p.getTotalQuestsCompleted(),
                    p.getTotalBlocksBroken(), online, p.isSpectating()));
        }

        ServerPlayNetworking.send(admin, new AdminDataPayload(players, ServerConfig.get().autoStart));
    }

    private static void handleAdminAction(ServerPlayerEntity admin, AdminActionPayload payload) {
        if (!admin.hasPermissionLevel(2)) return;
        OneBlockWorldState state = OneBlockWorldState.get(admin.server);

        switch (payload.action()) {
            case AdminActionPayload.TOGGLE_AUTO_START -> {
                ServerConfig config = ServerConfig.get();
                config.autoStart = !config.autoStart;
                ServerConfig.save();
                admin.sendMessage(Text.literal("Auto-start: " + (config.autoStart ? "ON" : "OFF"))
                        .formatted(Formatting.GOLD));
            }
            case AdminActionPayload.SET_PHASE -> {
                PlayerProgress tp = resolveProgressByName(admin.server, state, payload.targetPlayer());
                if (tp != null && tp.isStarted()) {
                    int newPhase = Math.max(1, Math.min(payload.value(), PhaseManager.getMaxPhase()));
                    tp.setCurrentPhase(newPhase);
                    state.markDirty();
                    // Notify online target
                    ServerPlayerEntity onlineTarget = admin.server.getPlayerManager().getPlayer(payload.targetPlayer());
                    if (onlineTarget != null) ModNetworking.syncQuestProgress(onlineTarget, tp);
                    admin.sendMessage(Text.literal("Set " + payload.targetPlayer() + " to phase " + newPhase)
                            .formatted(Formatting.GREEN));
                } else {
                    admin.sendMessage(Text.literal("Player not found or hasn't started.").formatted(Formatting.RED));
                }
            }
            case AdminActionPayload.RESET_PLAYER -> {
                PlayerProgress tp = resolveProgressByName(admin.server, state, payload.targetPlayer());
                if (tp != null) {
                    BlockPos savedPos = tp.getOneBlockPos();
                    tp.resetAll();
                    // Reuse existing island slot instead of allocating a new one
                    tp.setStarted(true);
                    tp.setCurrentPhase(1);
                    tp.setOneBlockPos(savedPos);
                    state.markDirty();
                    // If online, sync immediately
                    ServerPlayerEntity onlineTarget = admin.server.getPlayerManager().getPlayer(payload.targetPlayer());
                    if (onlineTarget != null) {
                        OneBlockCommand.initializeChallenge(onlineTarget);
                        ModNetworking.syncQuestProgress(onlineTarget, tp);
                    }
                    admin.sendMessage(Text.literal("Reset " + payload.targetPlayer()).formatted(Formatting.GREEN));
                } else {
                    admin.sendMessage(Text.literal("Player not found.").formatted(Formatting.RED));
                }
            }
        }
    }

    private static void handleTeamAction(ServerPlayerEntity player, TeamActionPayload payload) {
        String result;
        switch (payload.action()) {
            case TeamActionPayload.VIEW -> { sendTeamData(player); return; }
            case TeamActionPayload.CREATE -> result = TeamManager.createTeam(player.server, player, payload.target());
            case TeamActionPayload.INVITE -> result = TeamManager.invitePlayer(player.server, player, payload.target());
            case TeamActionPayload.ACCEPT -> result = TeamManager.acceptInvite(player.server, player, payload.target());
            case TeamActionPayload.DECLINE -> result = TeamManager.declineInvite(player.server, player, payload.target());
            case TeamActionPayload.LEAVE -> result = TeamManager.leaveTeam(player.server, player);
            case TeamActionPayload.KICK -> result = TeamManager.kickPlayer(player.server, player, payload.target());
            default -> result = "Unknown action.";
        }
        sendTeamDataWithMessage(player, result);
    }

    private static void sendTeamData(ServerPlayerEntity player) {
        sendTeamDataWithMessage(player, "");
    }

    private static void sendTeamDataWithMessage(ServerPlayerEntity player, String message) {
        OneBlockWorldState state = OneBlockWorldState.get(player.server);
        PlayerProgress progress = state.getProgress(player.getUuid());

        if (progress == null || progress.getTeamId() == null) {
            // Not in a team - send pending invites
            List<String> invites = new ArrayList<>();
            for (Team team : state.getAllTeams()) {
                if (team.hasInvite(player.getUuid())) {
                    String leaderName = resolveName(player.server, team.getLeaderId());
                    invites.add(leaderName);
                }
            }
            ServerPlayNetworking.send(player, new TeamDataPayload(
                    false, "", "", List.of(), invites, false, message));
        } else {
            Team team = state.getTeam(progress.getTeamId());
            if (team == null) {
                progress.setTeamId(null);
                ServerPlayNetworking.send(player, new TeamDataPayload(
                        false, "", "", List.of(), List.of(), false, message));
                return;
            }
            List<String> members = new ArrayList<>();
            for (UUID id : team.getMembers()) {
                members.add(resolveName(player.server, id));
            }
            List<String> invites = new ArrayList<>();
            for (UUID id : team.getPendingInvites()) {
                invites.add(resolveName(player.server, id));
            }
            ServerPlayNetworking.send(player, new TeamDataPayload(
                    true, team.getTeamName(), resolveName(player.server, team.getLeaderId()),
                    members, invites, team.isMergedIslands(), message));
        }
    }

    /** Find PlayerProgress by display name, works for both online and offline players. */
    private static PlayerProgress resolveProgressByName(MinecraftServer server,
                                                         OneBlockWorldState state, String name) {
        // Check online first (fast path)
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(name);
        if (online != null) return state.getProgress(online.getUuid());
        // Fall back: scan all progress entries and resolve names
        for (Map.Entry<UUID, PlayerProgress> entry : state.getAllProgress().entrySet()) {
            String resolved = resolveName(server, entry.getKey());
            if (resolved.equalsIgnoreCase(name)) return entry.getValue();
        }
        return null;
    }

    private static String resolveName(MinecraftServer server, UUID playerId) {
        return com.oneblocktoendall.util.PlayerNames.resolve(server, playerId);
    }
}
