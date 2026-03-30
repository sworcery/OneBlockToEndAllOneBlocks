package com.oneblocktoendall.event;

import com.oneblocktoendall.OneBlockMod;
import com.oneblocktoendall.block.ModBlocks;
import com.oneblocktoendall.block.OneBlock;
import com.oneblocktoendall.data.OneBlockWorldState;
import com.oneblocktoendall.network.ModNetworking;
import com.oneblocktoendall.phase.Phase;
import com.oneblocktoendall.phase.PhaseManager;
import com.oneblocktoendall.quest.PlayerProgress;
import com.oneblocktoendall.quest.Quest;
import com.oneblocktoendall.quest.QuestManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Runs every server tick. Handles critical game loop jobs:
 *
 * 1. ONE BLOCK REGENERATION — Safety net if block is somehow missing
 * 2. FALL SAFETY — Teleports players back if they fall too far below their block
 * 3. QUEST CHECKING — Every 20 ticks, poll stats for quest completion
 * 4. PHASE ADVANCEMENT — Advances phase with sound, particles, boss wave
 */
public class OneBlockTickHandler {

    private static int tickCounter = 0;
    private static final Random RANDOM = new Random();

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

        // --- Job 1: Regenerate broken one blocks (safety net backup) ---
        for (Map.Entry<UUID, PlayerProgress> entry : state.getAllProgress().entrySet()) {
            PlayerProgress progress = entry.getValue();
            if (!progress.isStarted() || progress.getCurrentPhase() < 1) continue;

            BlockPos pos = progress.getOneBlockPos();

            if (!(overworld.getBlockState(pos).getBlock() instanceof OneBlock)) {
                int phase = progress.getCurrentPhase();
                overworld.setBlockState(pos, ModBlocks.ONE_BLOCK.getDefaultState()
                        .with(OneBlock.PHASE, Math.min(phase, 25)));
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

        // --- Job 3: Check quest progress (every 20 ticks = 1 second) ---
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
                            // Play quest completion sound
                            player.getServerWorld().playSound(null, player.getBlockPos(),
                                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                                    SoundCategory.PLAYERS, 1.0f, 1.0f);
                            state.markDirty();
                        });
            }

            // Check if all quests in current phase are done
            if (!newlyCompleted.isEmpty() && QuestManager.isPhaseComplete(progress)) {
                if (!progress.isPhaseCompleteNotified()) {
                    advancePhase(player, progress, state);
                }
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

        // Show new quests
        for (Quest quest : nextPhase.quests()) {
            player.sendMessage(Text.literal(" \u2022 " + quest.name() + " - " + quest.description())
                    .formatted(Formatting.YELLOW));
        }
        player.sendMessage(Text.empty());

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
            EntityType<?> type = waveTypes[RANDOM.nextInt(waveTypes.length)];
            Entity entity = type.create(world, SpawnReason.EVENT);
            if (entity != null) {
                // Spawn around the block in a small radius
                double offsetX = (RANDOM.nextDouble() - 0.5) * 4;
                double offsetZ = (RANDOM.nextDouble() - 0.5) * 4;
                entity.refreshPositionAndAngles(
                        blockPos.getX() + 0.5 + offsetX,
                        blockPos.getY() + 1.0,
                        blockPos.getZ() + 0.5 + offsetZ,
                        RANDOM.nextFloat() * 360f, 0f);
                world.spawnEntity(entity);
            }
        }
    }
}
