package com.oneblocktoendall.block;

import com.oneblocktoendall.data.OneBlockWorldState;
import com.oneblocktoendall.phase.PhaseManager;
import com.oneblocktoendall.quest.PlayerProgress;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The One Block — the central block of the entire mod.
 *
 * It uses an integer block state property (PHASE, 1-10) to change its visual
 * appearance per phase. When broken, it does NOT drop itself — instead the
 * PhaseManager picks a random item from the current phase's loot pool.
 *
 * Features:
 * - Instant regeneration (no fall-through)
 * - Particle effects on every break
 * - Lucky drops (2% chance of a bonus rare item)
 * - Block break counter with milestone notifications
 */
public class OneBlock extends Block {

    /** Block state property controlling appearance. 1 = Survival, 25 = Ascension. */
    public static final IntProperty PHASE = IntProperty.of("phase", 1, 25);

    /** Lucky drop chance (2%) */
    private static final double LUCKY_DROP_CHANCE = 0.02;

    /** Block break milestones that trigger a notification. */
    private static final int[] MILESTONES = {100, 250, 500, 1000, 2500, 5000, 10000};

    public OneBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(PHASE, 1));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(PHASE);
    }

    /**
     * Called after the block is broken. We override to:
     * 1. Increment the mined stat (so quest tracking works)
     * 2. Apply hunger exhaustion (normal mining cost)
     * 3. Drop a random item from the phase pool (instead of the block itself)
     * 4. Maybe spawn a mob
     * 5. Check for lucky bonus drop
     * 6. Spawn particle effects
     * 7. Track block break count + milestones
     * 8. INSTANTLY regenerate the block so the player doesn't fall through
     *
     * We intentionally do NOT call super.afterBreak() to prevent the default
     * block drop (we don't want to drop a "one_block" item).
     */
    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos,
                           BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        // Increment "blocks mined" stat for this block (used by quest tracking)
        player.incrementStat(Stats.MINED.getOrCreateStat(this));
        // Apply normal mining exhaustion
        player.addExhaustion(0.005F);

        if (world instanceof ServerWorld serverWorld) {
            int phase = state.get(PHASE);

            // Drop a random item from this phase's loot pool
            PhaseManager.dropRandomItem(serverWorld, pos, phase);

            // Small chance to spawn a mob
            PhaseManager.trySpawnMob(serverWorld, pos, phase);

            // Lucky drop check (2% chance for a bonus item from the NEXT phase)
            if (ThreadLocalRandom.current().nextDouble() < LUCKY_DROP_CHANCE && phase < PhaseManager.getMaxPhase()) {
                PhaseManager.dropRandomItem(serverWorld, pos, phase + 1);
                // Golden sparkle particles + ding sound
                serverWorld.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        15, 0.3, 0.5, 0.3, 0.1);
                world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP,
                        SoundCategory.BLOCKS, 0.7f, 1.5f);
                if (player instanceof ServerPlayerEntity sp) {
                    sp.sendMessage(Text.literal("\u2728 Lucky Drop! Bonus item from the next phase!")
                            .formatted(Formatting.GOLD, Formatting.ITALIC));
                }
            }

            // Normal break particles (enchanted sparkle effect)
            serverWorld.spawnParticles(ParticleTypes.CRIT,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    8, 0.3, 0.3, 0.3, 0.1);

            // Block break counter + milestones
            if (player instanceof ServerPlayerEntity serverPlayer) {
                OneBlockWorldState worldState = OneBlockWorldState.get(serverPlayer.server);
                PlayerProgress progress = worldState.getProgress(serverPlayer.getUuid());
                if (progress != null && progress.isStarted()) {
                    int total = progress.incrementBlocksBroken();
                    worldState.markDirty();

                    // Check milestones
                    for (int milestone : MILESTONES) {
                        if (total == milestone) {
                            serverPlayer.sendMessage(Text.literal(
                                    "\uD83C\uDFC6 Milestone: " + milestone + " blocks broken!")
                                    .formatted(Formatting.AQUA, Formatting.BOLD));
                            world.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                                    SoundCategory.PLAYERS, 1.0f, 1.0f);
                            break;
                        }
                    }
                }
            }

            // INSTANT REGENERATION — place the block back immediately
            int currentPhase = phase;
            if (player instanceof ServerPlayerEntity serverPlayer) {
                OneBlockWorldState worldState = OneBlockWorldState.get(serverPlayer.server);
                PlayerProgress progress = worldState.getProgress(serverPlayer.getUuid());
                if (progress != null && progress.isStarted()) {
                    currentPhase = progress.getCurrentPhase();
                }
            }
            world.setBlockState(pos, ModBlocks.ONE_BLOCK.getDefaultState()
                    .with(PHASE, Math.min(currentPhase, 25)));
        }
    }

}
