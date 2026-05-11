package com.oneblocktoendall.block;

import com.oneblocktoendall.data.OneBlockWorldState;
import com.oneblocktoendall.quest.PlayerProgress;
import com.oneblocktoendall.team.Team;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TeleporterBlock extends Block {

    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TICKS = 600; // 30 seconds

    public TeleporterBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                  PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) return ActionResult.SUCCESS;

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        MinecraftServer server = serverPlayer.server;
        OneBlockWorldState worldState = OneBlockWorldState.get(server);
        PlayerProgress progress = worldState.getProgress(serverPlayer.getUuid());

        if (progress == null || !progress.isStarted()) {
            serverPlayer.sendMessage(Text.literal("You haven't started the challenge!")
                    .formatted(Formatting.RED));
            return ActionResult.SUCCESS;
        }

        if (progress.getTeamId() == null) {
            serverPlayer.sendMessage(Text.literal("You need to be in a team to use the teleporter!")
                    .formatted(Formatting.RED));
            return ActionResult.SUCCESS;
        }

        Team team = worldState.getTeam(progress.getTeamId());
        if (team == null || !team.isMergedIslands()) {
            serverPlayer.sendMessage(Text.literal("Your team's islands must be merged first!")
                    .formatted(Formatting.RED));
            return ActionResult.SUCCESS;
        }

        long currentTick = world.getTime();
        Long lastUse = cooldowns.get(serverPlayer.getUuid());
        if (lastUse != null && currentTick - lastUse < COOLDOWN_TICKS) {
            long remaining = (COOLDOWN_TICKS - (currentTick - lastUse)) / 20;
            serverPlayer.sendMessage(Text.literal("Teleporter on cooldown! " + remaining + "s remaining.")
                    .formatted(Formatting.YELLOW));
            return ActionResult.SUCCESS;
        }

        // Find a teammate to teleport to
        UUID targetId = null;
        for (UUID memberId : team.getMembers()) {
            if (!memberId.equals(serverPlayer.getUuid())) {
                targetId = memberId;
                break;
            }
        }

        if (targetId == null) {
            serverPlayer.sendMessage(Text.literal("No teammates found!")
                    .formatted(Formatting.RED));
            return ActionResult.SUCCESS;
        }

        PlayerProgress targetProgress = worldState.getProgress(targetId);
        if (targetProgress == null || !targetProgress.isStarted()) {
            serverPlayer.sendMessage(Text.literal("Teammate hasn't started the challenge!")
                    .formatted(Formatting.RED));
            return ActionResult.SUCCESS;
        }

        BlockPos targetPos = targetProgress.getOneBlockPos();
        ServerWorld serverWorld = serverPlayer.getServerWorld();

        // Departure effects
        serverWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                30, 0.3, 0.5, 0.3, 0.05);
        serverWorld.playSound(null, pos, SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 1.0f, 1.2f);

        // Teleport
        serverPlayer.teleport(serverWorld,
                targetPos.getX() + 2.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
                Set.of(), serverPlayer.getYaw(), serverPlayer.getPitch(), false);

        // Arrival effects
        serverWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                targetPos.getX() + 2.5, targetPos.getY() + 1.5, targetPos.getZ() + 0.5,
                30, 0.3, 0.5, 0.3, 0.05);
        serverWorld.playSound(null, targetPos, SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 1.0f, 0.8f);

        cooldowns.put(serverPlayer.getUuid(), currentTick);

        String targetName = com.oneblocktoendall.util.PlayerNames.resolve(server, targetId);
        serverPlayer.sendMessage(Text.literal("Teleported to " + targetName + "'s island!")
                .formatted(Formatting.LIGHT_PURPLE));

        return ActionResult.SUCCESS;
    }
}
