package com.oneblocktoendall.event;

import com.oneblocktoendall.OneBlockMod;
import com.oneblocktoendall.block.ModBlocks;
import com.oneblocktoendall.block.OneBlock;
import com.oneblocktoendall.data.OneBlockWorldState;
import com.oneblocktoendall.network.ModNetworking;
import com.oneblocktoendall.network.PhaseAdvancePayload;
import com.oneblocktoendall.network.QuestSyncPayload;
import com.oneblocktoendall.network.ToastPayload;
import com.oneblocktoendall.phase.Phase;
import com.oneblocktoendall.phase.PhaseManager;
import com.oneblocktoendall.quest.PlayerProgress;
import com.oneblocktoendall.quest.Quest;
import com.oneblocktoendall.quest.QuestManager;
import com.oneblocktoendall.team.Team;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Runs every server tick. Handles critical game loop jobs:
 *
 * 1. ONE BLOCK REGENERATION — Safety net if block is somehow missing
 * 2. FALL SAFETY — Teleports players back if they fall too far below their block
 * 3. QUEST CHECKING — Every 20 ticks, poll stats for quest completion
 * 4. PHASE ADVANCEMENT — Advances phase with sound, particles, boss wave
 */
public class OneBlockTickHandler {

    private static long tickCounter = 0;

    /** How far below the one block a player can fall before being teleported back. */
    private static final int FALL_SAFETY_DISTANCE = 100;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(OneBlockTickHandler::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        tickCounter++;

        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) return;

        OneBlockWorldState state = OneBlockWorldState.get(server);

        // --- Job 1: Regenerate broken one blocks (online players only — no point loading chunks for offline players) ---
        for (ServerPlayerEntity onlinePlayer : server.getPlayerManager().getPlayerList()) {
            PlayerProgress progress = state.getProgress(onlinePlayer.getUuid());
            if (progress == null || !progress.isStarted() || progress.getCurrentPhase() < 1) continue;

            BlockPos pos = progress.getOneBlockPos();
            if (overworld.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)
                    && !(overworld.getBlockState(pos).getBlock() instanceof OneBlock)) {
                overworld.setBlockState(pos, ModBlocks.ONE_BLOCK.getDefaultState()
                        .with(OneBlock.PHASE, Math.min(progress.getCurrentPhase(), 25)));
            }
        }

        // --- Job 2: Fall safety — teleport players back if they fall too far ---
        if (tickCounter % 5 == 0) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                PlayerProgress progress = state.getProgress(player.getUuid());
                if (progress == null || !progress.isStarted()) continue;

                BlockPos blockPos = progress.getOneBlockPos();
                double safetyY = blockPos.getY() - FALL_SAFETY_DISTANCE;

                if (player.getY() < safetyY) {
                    // Teleport player back on top of their one block
                    player.teleport(overworld,
                            blockPos.getX() + 0.5, blockPos.getY() + 1.0, blockPos.getZ() + 0.5,
                            java.util.Set.of(), player.getYaw(), player.getPitch(), false);
                    player.setVelocity(0, 0, 0);
                    player.velocityModified = true;
                    player.sendMessage(Text.literal("You fell! Teleported back to your block.")
                            .formatted(Formatting.YELLOW));
                }
            }
        }

        // --- Job 3: Check bridge connections between islands (every 200 ticks = 10 seconds) ---
        if (tickCounter % 200 == 0) {
            checkBridgeConnections(server, state, overworld);
        }

        // --- Job 4: Check quest progress (every 20 ticks = 1 second) ---
        if (tickCounter % 20 != 0) return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerProgress progress = state.getProgress(player.getUuid());
            if (progress == null || !progress.isStarted() || progress.getCurrentPhase() < 1) {
                continue;
            }

            // Check quests and get newly completed ones
            List<String> newlyCompleted = QuestManager.checkQuests(player, progress);

            // Notify player of completed quests with sound effect
            for (String questId : newlyCompleted) {
                Phase phase = PhaseManager.getPhase(progress.getCurrentPhase());
                if (phase == null) continue;

                phase.quests().stream()
                        .filter(q -> q.id().equals(questId))
                        .findFirst()
                        .ifPresent(quest -> {
                            player.sendMessage(Text.literal("\u2714 Quest Complete: " + quest.name())
                                    .formatted(Formatting.GREEN, Formatting.BOLD));
                            player.getServerWorld().playSound(null, player.getBlockPos(),
                                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                                    SoundCategory.PLAYERS, 1.0f, 1.0f);
                            // Send toast notification
                            ModNetworking.sendToast(player, new ToastPayload(
                                    ToastPayload.TYPE_QUEST, player.getName().getString(),
                                    "", quest.name(), 0));
                            state.markDirty();
                        });
            }

            // Check if all quests in current phase are done — checked unconditionally so players
            // who log out on a completed phase still advance when they log back in.
            if (QuestManager.isPhaseComplete(progress) && !progress.isPhaseCompleteNotified()) {
                advancePhase(player, progress, state);
            }

            // Sync progress to client for HUD display
            if (!newlyCompleted.isEmpty() || tickCounter % 100 == 0) {
                ModNetworking.syncQuestProgress(player, progress);
            }
        }
    }

    /**
     * Advance the player to the next phase.
     * Plays fanfare sound, spawns firework particles, and triggers a boss wave.
     */
    private static void advancePhase(ServerPlayerEntity player, PlayerProgress progress,
                                      OneBlockWorldState state) {
        int currentPhase = progress.getCurrentPhase();
        int maxPhase = PhaseManager.getMaxPhase();

        if (currentPhase >= maxPhase) {
            // Completed all phases!
            progress.setPhaseCompleteNotified(true);
            state.markDirty();

            player.sendMessage(Text.empty());
            player.sendMessage(Text.literal("========================================")
                    .formatted(Formatting.GOLD));
            player.sendMessage(Text.literal("  CONGRATULATIONS! You've completed")
                    .formatted(Formatting.GOLD, Formatting.BOLD));
            player.sendMessage(Text.literal("  One Block to Rule Them All!")
                    .formatted(Formatting.GOLD, Formatting.BOLD));
            player.sendMessage(Text.literal("  Total blocks broken: " + progress.getTotalBlocksBroken())
                    .formatted(Formatting.AQUA));
            player.sendMessage(Text.literal("========================================")
                    .formatted(Formatting.GOLD));
            player.sendMessage(Text.empty());

            // Victory fanfare
            ServerWorld world = player.getServerWorld();
            world.playSound(null, player.getBlockPos(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                    SoundCategory.PLAYERS, 1.0f, 1.0f);
            // Big firework particle burst
            world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    100, 1.0, 1.5, 1.0, 0.3);
            return;
        }

        // Advance to next phase
        int newPhase = currentPhase + 1;
        progress.setCurrentPhase(newPhase);
        state.markDirty();

        Phase nextPhase = PhaseManager.getPhase(newPhase);
        if (nextPhase == null) return;

        // Update the one block's appearance
        BlockPos pos = progress.getOneBlockPos();
        ServerWorld world = player.getServerWorld();
        if (world.getBlockState(pos).getBlock() instanceof OneBlock) {
            world.setBlockState(pos, ModBlocks.ONE_BLOCK.getDefaultState()
                    .with(OneBlock.PHASE, Math.min(newPhase, 25)));
        }

        // Phase advancement fanfare sound + particles
        world.playSound(null, player.getBlockPos(),
                SoundEvents.ENTITY_PLAYER_LEVELUP,
                SoundCategory.PLAYERS, 1.0f, 1.0f);
        world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                40, 0.5, 1.0, 0.5, 0.2);

        // Announce phase change
        player.sendMessage(Text.empty());
        player.sendMessage(Text.literal("=== Phase " + newPhase + ": " + nextPhase.name() + " ===")
                .formatted(Formatting.GOLD, Formatting.BOLD));
        player.sendMessage(Text.literal("New blocks and mobs are now available!")
                .formatted(Formatting.GREEN));
        player.sendMessage(Text.literal("Press J to see your new objectives.")
                .formatted(Formatting.GRAY));
        player.sendMessage(Text.empty());

        // Show new quests in chat
        for (Quest quest : nextPhase.quests()) {
            player.sendMessage(Text.literal(" \u2022 " + quest.name() + " - " + quest.description())
                    .formatted(Formatting.YELLOW));
        }
        player.sendMessage(Text.empty());

        // Send phase celebration screen data
        List<String> newBlocks = PhaseManager.getNewItemsForPhase(newPhase);
        List<String> newMobs = PhaseManager.getNewMobsForPhase(newPhase);
        List<QuestSyncPayload.QuestStatus> questStatuses = new java.util.ArrayList<>();
        for (Quest quest : nextPhase.quests()) {
            questStatuses.add(new QuestSyncPayload.QuestStatus(
                    quest.id(), quest.name(), quest.description(), 0, quest.count(), false));
        }
        ServerPlayNetworking.send(player, new PhaseAdvancePayload(
                newPhase, nextPhase.name(), newBlocks, newMobs, questStatuses));

        // Broadcast phase toast to all players
        String playerName = player.getName().getString();
        ModNetworking.broadcastToast(player.server, new ToastPayload(
                ToastPayload.TYPE_PHASE_BROADCAST, playerName, nextPhase.name(), "", newPhase));
        // Personal toast
        ModNetworking.sendToast(player, new ToastPayload(
                ToastPayload.TYPE_PHASE, playerName, nextPhase.name(), "", newPhase));

        // --- BOSS WAVE --- spawn a hostile mob every 5th phase completion
        if (currentPhase % 5 == 0) {
            spawnBossWave(world, pos, currentPhase);

            player.sendMessage(Text.literal("\u26A0 Boss Wave incoming! Defend yourself!")
                    .formatted(Formatting.RED, Formatting.BOLD));
            world.playSound(null, pos, SoundEvents.ENTITY_ENDER_DRAGON_GROWL,
                    SoundCategory.HOSTILE, 0.6f, 1.2f);
        }
    }

    /**
     * Spawn a wave of hostile mobs near the one block as a phase transition challenge.
     * The wave difficulty scales with the completed phase number.
     */
    private static void spawnBossWave(ServerWorld world, BlockPos blockPos, int completedPhase) {
        // Boss wave composition scales with phase
        EntityType<?>[] waveTypes;
        int count;

        switch (completedPhase) {
            case 5 -> { waveTypes = new EntityType[]{EntityType.WITCH}; count = 1; }
            case 10 -> { waveTypes = new EntityType[]{EntityType.VINDICATOR}; count = 1; }
            case 15 -> { waveTypes = new EntityType[]{EntityType.EVOKER}; count = 1; }
            case 20 -> { waveTypes = new EntityType[]{EntityType.WARDEN}; count = 1; }
            default -> { waveTypes = new EntityType[]{EntityType.RAVAGER}; count = 1; }
        }

        for (int i = 0; i < count; i++) {
            EntityType<?> type = waveTypes[ThreadLocalRandom.current().nextInt(waveTypes.length)];
            Entity entity = type.create(world, SpawnReason.EVENT);
            if (entity != null) {
                // Spawn around the block in a small radius
                double offsetX = (ThreadLocalRandom.current().nextDouble() - 0.5) * 4;
                double offsetZ = (ThreadLocalRandom.current().nextDouble() - 0.5) * 4;
                entity.refreshPositionAndAngles(
                        blockPos.getX() + 0.5 + offsetX,
                        blockPos.getY() + 1.0,
                        blockPos.getZ() + 0.5 + offsetZ,
                        ThreadLocalRandom.current().nextFloat() * 360f, 0f);
                world.spawnEntity(entity);
            }
        }
    }

    /**
     * Check all teams for a physical block bridge connecting two members' islands.
     * Called every 200 ticks (~10 seconds). Only checks teams that haven't merged yet.
     */
    private static void checkBridgeConnections(MinecraftServer server, OneBlockWorldState state,
                                                ServerWorld world) {
        for (Team team : state.getAllTeams()) {
            if (team.isMergedIslands()) continue;
            if (team.getMembers().size() < 2) continue;

            List<PlayerProgress> started = team.getMembers().stream()
                    .map(state::getProgress)
                    .filter(p -> p != null && p.isStarted() && p.getCurrentPhase() >= 1)
                    .collect(Collectors.toList());

            if (started.size() < 2) continue;

            // Check each pair of members for a bridge
            for (int i = 0; i < started.size(); i++) {
                for (int j = i + 1; j < started.size(); j++) {
                    PlayerProgress a = started.get(i);
                    PlayerProgress b = started.get(j);
                    if (hasBridgeConnection(world, a.getOneBlockPos(), b.getOneBlockPos())) {
                        triggerIslandMerge(server, state, world, team, started);
                        return; // Only merge one team per check cycle
                    }
                }
            }
        }
    }

    /**
     * BFS flood-fill from island A through solid blocks to see if island B is reachable.
     *
     * Limits: 25000 blocks visited (generous for a 250-block bridge with platform builds),
     * seed radius 8 around island A, target detection radius 12 around island B.
     */
    private static boolean hasBridgeConnection(ServerWorld world, BlockPos posA, BlockPos posB) {
        int yMin = Math.min(posA.getY(), posB.getY()) - 20;
        int yMax = Math.max(posA.getY(), posB.getY()) + 50;
        int targetRadiusSq = 12 * 12;
        int maxVisited = 25000;

        Set<Long> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        // Seed BFS from non-air blocks within radius 8 of island A
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -4; dy <= 8; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    BlockPos seed = posA.add(dx, dy, dz);
                    if (!world.getBlockState(seed).isAir() && visited.add(seed.asLong())) {
                        queue.add(seed);
                    }
                }
            }
        }

        while (!queue.isEmpty() && visited.size() < maxVisited) {
            BlockPos current = queue.poll();

            // Check if we've reached island B's vicinity
            int ddx = current.getX() - posB.getX();
            int ddy = current.getY() - posB.getY();
            int ddz = current.getZ() - posB.getZ();
            if (ddx * ddx + ddy * ddy + ddz * ddz <= targetRadiusSq) return true;

            // Expand to 6 adjacent non-air blocks
            BlockPos[] neighbors = {
                current.north(), current.south(), current.east(),
                current.west(), current.up(), current.down()
            };
            for (BlockPos neighbor : neighbors) {
                if (neighbor.getY() < yMin || neighbor.getY() > yMax) continue;
                if (!visited.add(neighbor.asLong())) continue;
                if (!world.getBlockState(neighbor).isAir()) {
                    queue.add(neighbor);
                }
            }
        }
        return false;
    }

    /**
     * Called when a bridge is detected. Sets the team as merged, bumps all members
     * to max(currentPhases) + 1, notifies everyone, and broadcasts server-wide.
     */
    private static void triggerIslandMerge(MinecraftServer server, OneBlockWorldState state,
                                            ServerWorld world, Team team,
                                            List<PlayerProgress> memberProgress) {
        team.setMergedIslands(true);

        // Everyone advances to the highest current phase + 1 (capped at max)
        int maxPhase = PhaseManager.getMaxPhase();
        int targetPhase = memberProgress.stream()
                .mapToInt(PlayerProgress::getCurrentPhase)
                .max().orElse(1);
        targetPhase = Math.min(targetPhase + 1, maxPhase);

        Phase newPhaseObj = PhaseManager.getPhase(targetPhase);
        String phaseName = newPhaseObj != null ? newPhaseObj.name() : "Phase " + targetPhase;

        List<String> newBlocks = PhaseManager.getNewItemsForPhase(targetPhase);
        List<String> newMobs = PhaseManager.getNewMobsForPhase(targetPhase);
        List<QuestSyncPayload.QuestStatus> questStatuses = new ArrayList<>();
        if (newPhaseObj != null) {
            for (Quest quest : newPhaseObj.quests()) {
                questStatuses.add(new QuestSyncPayload.QuestStatus(
                        quest.id(), quest.name(), quest.description(), 0, quest.count(), false));
            }
        }

        final int finalTargetPhase = targetPhase;
        for (PlayerProgress progress : memberProgress) {
            progress.setCurrentPhase(finalTargetPhase); // resets phaseCompleteNotified

            // Update the one block visual
            BlockPos pos = progress.getOneBlockPos();
            if (world.getBlockState(pos).getBlock() instanceof OneBlock) {
                world.setBlockState(pos, ModBlocks.ONE_BLOCK.getDefaultState()
                        .with(OneBlock.PHASE, Math.min(finalTargetPhase, 25)));
            }

            // Notify the player if they're online
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(progress.getPlayerId());
            if (player != null) {
                player.sendMessage(Text.empty());
                player.sendMessage(Text.literal("\u2605 ISLANDS MERGED! \u2605")
                        .formatted(Formatting.GOLD, Formatting.BOLD));
                player.sendMessage(Text.literal("Your bridge connected the islands!")
                        .formatted(Formatting.GREEN));
                player.sendMessage(Text.literal("Advanced to Phase " + finalTargetPhase + ": " + phaseName)
                        .formatted(Formatting.AQUA, Formatting.BOLD));
                player.sendMessage(Text.literal("Use /oneblock visit <player> to teleport to your ally's island.")
                        .formatted(Formatting.YELLOW));
                player.sendMessage(Text.empty());

                world.playSound(null, player.getBlockPos(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                        SoundCategory.PLAYERS, 1.0f, 0.8f);
                world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        80, 1.0, 1.5, 1.0, 0.3);

                ModNetworking.sendToast(player, new ToastPayload(
                        ToastPayload.TYPE_PHASE, player.getName().getString(),
                        "Islands Merged!", "", finalTargetPhase));

                ServerPlayNetworking.send(player, new PhaseAdvancePayload(
                        finalTargetPhase, "\u2605 Islands Merged! " + phaseName,
                        newBlocks, newMobs, questStatuses));

                ModNetworking.syncQuestProgress(player, progress);
            }
        }

        state.markDirty();

        // Server-wide announcement
        server.getPlayerManager().broadcast(
                Text.literal("\u2605 " + team.getTeamName() + "'s islands are now connected! They advanced to Phase "
                        + finalTargetPhase + "! \u2605")
                        .formatted(Formatting.GOLD),
                false);
    }
}
