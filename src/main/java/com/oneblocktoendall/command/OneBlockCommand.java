package com.oneblocktoendall.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.oneblocktoendall.block.ModBlocks;
import com.oneblocktoendall.block.OneBlock;
import com.oneblocktoendall.data.OneBlockWorldState;
import com.oneblocktoendall.network.AdminRequestPayload;
import com.oneblocktoendall.network.ModNetworking;
import com.oneblocktoendall.phase.Phase;
import com.oneblocktoendall.phase.PhaseManager;
import com.oneblocktoendall.quest.PlayerProgress;
import com.oneblocktoendall.quest.Quest;
import com.oneblocktoendall.quest.QuestManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The /oneblock command — the player's interface for managing their challenge.
 *
 * The challenge auto-starts when a player first joins a world.
 * Commands are for info and reset only:
 *   /oneblock quests — Show current quest progress in chat
 *   /oneblock phase  — Show current phase info
 *   /oneblock reset  — Reset progress and start over
 */
public class OneBlockCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("oneblock")
                .then(CommandManager.literal("start")
                        .executes(OneBlockCommand::startChallenge))
                .then(CommandManager.literal("quests")
                        .executes(OneBlockCommand::showQuests))
                .then(CommandManager.literal("phase")
                        .executes(OneBlockCommand::showPhase))
                .then(CommandManager.literal("reset")
                        .executes(OneBlockCommand::resetChallenge))
                .then(CommandManager.literal("leaderboard")
                        .executes(OneBlockCommand::showLeaderboard))
                .then(CommandManager.literal("visit")
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .executes(OneBlockCommand::visitPlayer)))
                .then(CommandManager.literal("admin")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(OneBlockCommand::openAdmin))
        );
    }

    private static int startChallenge(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;
        initializeChallenge(player);
        return 1;
    }

    /**
     * Initialize the one block challenge for a player.
     * Called automatically on first join (from OneBlockMod) or after a reset.
     * Safe to call multiple times — skips if already started.
     *
     * Places the one block at Y=200 and clears a large void pocket around it
     * so the mod works with ANY world type (normal, flat, custom, etc.).
     */
    /** Distance between player islands in blocks. 250 in honor of America's 250th. */
    private static final int ISLAND_SPACING = 250;

    public static void initializeChallenge(ServerPlayerEntity player) {
        OneBlockWorldState state = OneBlockWorldState.get(player.server);
        PlayerProgress progress = state.getOrCreateProgress(player.getUuid());

        if (progress.isStarted()) return;

        ServerWorld world = player.getServerWorld();

        // Each player gets a unique island position spread 500 blocks apart
        // Player index is based on how many players have ever started
        int playerIndex = (int) state.getAllProgress().values().stream()
                .filter(p -> p.isStarted() || p.getPlayerId().equals(player.getUuid()))
                .count();
        // Spiral layout: place islands along X axis spaced apart
        // Player 0 -> (0, 200, 0), Player 1 -> (500, 200, 0), Player 2 -> (-500, 200, 0), etc.
        int x;
        if (playerIndex <= 1) {
            x = 0; // First player at origin
        } else {
            int offset = (playerIndex / 2) * ISLAND_SPACING;
            x = (playerIndex % 2 == 0) ? offset : -offset;
        }
        BlockPos blockPos = new BlockPos(x, 200, 0);

        // Clear a void pocket around the block position (21x31x21 area)
        clearArea(world, blockPos, 10, 10, 20);

        // Now place the one block
        world.setBlockState(blockPos, ModBlocks.ONE_BLOCK.getDefaultState()
                .with(OneBlock.PHASE, 1));

        // Initialize player progress
        progress.setStarted(true);
        progress.setCurrentPhase(1);
        progress.setOneBlockPos(blockPos);
        progress.setSpectating(false);
        progress.setChallengeStartTime(world.getTime());
        state.markDirty();

        // Teleport player on top of the one block
        player.teleport(world, blockPos.getX() + 0.5, blockPos.getY() + 1.0,
                blockPos.getZ() + 0.5, Set.of(), player.getYaw(), player.getPitch(), true);

        // Set spawn point so they respawn here
        player.setSpawnPoint(world.getRegistryKey(), blockPos.up(), 0f, true, false);

        // Give starter kit — water bucket + bread to survive the first few minutes
        player.getInventory().insertStack(new ItemStack(Items.WATER_BUCKET, 1));
        player.getInventory().insertStack(new ItemStack(Items.BREAD, 3));

        // Welcome message
        player.sendMessage(Text.empty());
        player.sendMessage(Text.literal("=== One Block to Rule Them All ===")
                .formatted(Formatting.GOLD, Formatting.BOLD));
        player.sendMessage(Text.literal("Your challenge begins! Break the block beneath you.")
                .formatted(Formatting.GREEN));
        player.sendMessage(Text.literal("Complete quests to unlock new phases and resources.")
                .formatted(Formatting.GRAY));
        player.sendMessage(Text.literal("Press J to open your Quest Book.")
                .formatted(Formatting.GRAY));
        player.sendMessage(Text.empty());

        // Patriotic red/white/blue firework celebration
        launchPatrioticFireworks(world, blockPos);
    }

    /**
     * Launch red, white, and blue fireworks around a position.
     * Celebrates America's 250th — happy birthday!
     */
    private static void launchPatrioticFireworks(ServerWorld world, BlockPos center) {
        Random rand = new Random();
        int[][] colors = {
                {DyeColor.RED.getFireworkColor()},
                {DyeColor.WHITE.getFireworkColor()},
                {DyeColor.BLUE.getFireworkColor()},
                {DyeColor.RED.getFireworkColor(), DyeColor.WHITE.getFireworkColor()},
                {DyeColor.WHITE.getFireworkColor(), DyeColor.BLUE.getFireworkColor()},
                {DyeColor.RED.getFireworkColor(), DyeColor.BLUE.getFireworkColor()},
        };

        FireworkExplosionComponent.Type[] shapes = {
                FireworkExplosionComponent.Type.LARGE_BALL,
                FireworkExplosionComponent.Type.STAR,
                FireworkExplosionComponent.Type.BURST,
        };

        for (int i = 0; i < 7; i++) {
            double offsetX = (rand.nextDouble() - 0.5) * 8;
            double offsetZ = (rand.nextDouble() - 0.5) * 8;

            int[] colorSet = colors[i % colors.length];
            IntList colorList = new IntArrayList();
            for (int c : colorSet) colorList.add(c);
            IntList fadeList = new IntArrayList();
            fadeList.add(DyeColor.WHITE.getFireworkColor());

            FireworkExplosionComponent explosion = new FireworkExplosionComponent(
                    shapes[i % shapes.length],
                    colorList,
                    fadeList,
                    true,  // trail
                    true   // twinkle
            );

            ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
            rocket.set(DataComponentTypes.FIREWORKS,
                    new FireworksComponent(1 + rand.nextInt(2), List.of(explosion)));

            FireworkRocketEntity entity = new FireworkRocketEntity(world,
                    center.getX() + 0.5 + offsetX,
                    center.getY() + 2.0,
                    center.getZ() + 0.5 + offsetZ,
                    rocket);
            world.spawnEntity(entity);
        }
    }

    /**
     * /oneblock quests — Display current quest progress.
     */
    private static int showQuests(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        OneBlockWorldState state = OneBlockWorldState.get(player.server);
        PlayerProgress progress = state.getProgress(player.getUuid());

        if (progress == null || !progress.isStarted()) {
            player.sendMessage(Text.literal("You haven't started the challenge yet! Use /oneblock start")
                    .formatted(Formatting.RED));
            return 0;
        }

        Phase phase = PhaseManager.getPhase(progress.getCurrentPhase());
        if (phase == null) return 0;

        player.sendMessage(Text.empty());
        player.sendMessage(Text.literal("=== Phase " + phase.id() + ": " + phase.name() + " ===")
                .formatted(Formatting.GOLD, Formatting.BOLD));

        for (Quest quest : phase.quests()) {
            boolean completed = progress.isQuestCompleted(quest.id());
            int questProgress = QuestManager.getQuestProgress(player, quest, progress);

            Formatting color = completed ? Formatting.GREEN : Formatting.WHITE;
            String checkmark = completed ? " \u2714 " : " \u2022 ";
            String progressText = completed ? "" :
                    " [" + questProgress + "/" + quest.count() + "]";

            player.sendMessage(Text.literal(checkmark + quest.name() + progressText)
                    .formatted(color)
                    .append(Text.literal(" - " + quest.description())
                            .formatted(Formatting.GRAY)));
        }

        if (QuestManager.isPhaseComplete(progress)) {
            if (progress.getCurrentPhase() < PhaseManager.getMaxPhase()) {
                player.sendMessage(Text.literal("\nAll quests complete! Advancing to next phase...")
                        .formatted(Formatting.GREEN, Formatting.BOLD));
            } else {
                player.sendMessage(Text.literal("\nYou've completed ALL phases! Congratulations!")
                        .formatted(Formatting.GOLD, Formatting.BOLD));
            }
        }

        player.sendMessage(Text.empty());
        return 1;
    }

    /**
     * /oneblock phase — Show current phase info.
     */
    private static int showPhase(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        OneBlockWorldState state = OneBlockWorldState.get(player.server);
        PlayerProgress progress = state.getProgress(player.getUuid());

        if (progress == null || !progress.isStarted()) {
            player.sendMessage(Text.literal("You haven't started the challenge yet! Use /oneblock start")
                    .formatted(Formatting.RED));
            return 0;
        }

        Phase phase = PhaseManager.getPhase(progress.getCurrentPhase());
        if (phase == null) return 0;

        int completed = (int) phase.quests().stream()
                .filter(q -> progress.isQuestCompleted(q.id())).count();
        int total = phase.quests().size();

        player.sendMessage(Text.empty());
        player.sendMessage(Text.literal("Phase " + phase.id() + "/" + PhaseManager.getMaxPhase()
                        + ": " + phase.name())
                .formatted(Formatting.GOLD, Formatting.BOLD));
        player.sendMessage(Text.literal("Quests: " + completed + "/" + total + " completed")
                .formatted(completed == total ? Formatting.GREEN : Formatting.YELLOW));
        player.sendMessage(Text.literal("Block pool: " + phase.blockPool().size() + " items")
                .formatted(Formatting.GRAY));
        player.sendMessage(Text.literal("Mob spawn chance: " +
                        (int)(phase.mobSpawnChance() * 100) + "%")
                .formatted(Formatting.GRAY));
        player.sendMessage(Text.empty());

        return 1;
    }

    /**
     * /oneblock reset — Reset the player's challenge progress.
     */
    private static int resetChallenge(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        OneBlockWorldState state = OneBlockWorldState.get(player.server);
        PlayerProgress progress = state.getProgress(player.getUuid());

        if (progress == null || !progress.isStarted()) {
            player.sendMessage(Text.literal("Nothing to reset — you haven't started yet!")
                    .formatted(Formatting.YELLOW));
            return 0;
        }

        // Save block position before reset so we reuse the same island spot
        BlockPos savedPos = progress.getOneBlockPos();
        progress.resetAll();
        state.markDirty();

        // Clear the area at the existing island position
        ServerWorld world = player.getServerWorld();
        clearArea(world, savedPos, 10, 10, 20);

        // Re-initialize at the same position (not a new slot)
        progress.setStarted(true);
        progress.setCurrentPhase(1);
        progress.setOneBlockPos(savedPos);
        progress.setSpectating(false);
        progress.setChallengeStartTime(world.getTime());
        state.markDirty();

        world.setBlockState(savedPos, ModBlocks.ONE_BLOCK.getDefaultState()
                .with(OneBlock.PHASE, 1));

        player.teleport(world, savedPos.getX() + 0.5, savedPos.getY() + 1.0,
                savedPos.getZ() + 0.5, Set.of(), player.getYaw(), player.getPitch(), true);
        player.setSpawnPoint(world.getRegistryKey(), savedPos.up(), 0f, true, false);

        player.getInventory().insertStack(new ItemStack(Items.WATER_BUCKET, 1));
        player.getInventory().insertStack(new ItemStack(Items.BREAD, 3));

        player.sendMessage(Text.literal("Challenge reset! Starting fresh at your island.")
                .formatted(Formatting.GREEN));

        return 1;
    }

    private static int showLeaderboard(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;
        OneBlockWorldState state = OneBlockWorldState.get(player.server);
        var entries = state.buildLeaderboard(player.server);
        player.sendMessage(Text.empty());
        player.sendMessage(Text.literal("=== Leaderboard ===").formatted(Formatting.GOLD, Formatting.BOLD));
        for (int i = 0; i < entries.size(); i++) {
            var e = entries.get(i);
            Formatting color = e.name().equals(player.getName().getString()) ? Formatting.GOLD : Formatting.WHITE;
            player.sendMessage(Text.literal((i + 1) + ". " + e.name() +
                    " - Phase " + e.phase() + " | " + e.questsCompleted() + " quests | " +
                    e.blocksBroken() + " blocks").formatted(color));
        }
        player.sendMessage(Text.empty());
        return 1;
    }

    private static int visitPlayer(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;
        String targetName = StringArgumentType.getString(context, "player");
        ServerPlayerEntity target = player.server.getPlayerManager().getPlayer(targetName);
        if (target == null) {
            player.sendMessage(Text.literal("Player not found or offline!").formatted(Formatting.RED));
            return 0;
        }
        OneBlockWorldState state = OneBlockWorldState.get(player.server);
        PlayerProgress targetProgress = state.getProgress(target.getUuid());
        if (targetProgress == null || !targetProgress.isStarted()) {
            player.sendMessage(Text.literal("That player hasn't started the challenge!").formatted(Formatting.RED));
            return 0;
        }
        if (!targetProgress.isAllowVisitors()) {
            player.sendMessage(Text.literal("That player doesn't allow visitors!").formatted(Formatting.RED));
            return 0;
        }
        BlockPos pos = targetProgress.getOneBlockPos();
        player.teleport(player.getServerWorld(),
                pos.getX() + 2.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                Set.of(), player.getYaw(), player.getPitch(), false);
        player.sendMessage(Text.literal("Teleported to " + targetName + "'s island!").formatted(Formatting.GREEN));
        return 1;
    }

    private static int openAdmin(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;
        player.sendMessage(Text.literal("Opening admin panel... Press J > Gear to use the GUI.")
                .formatted(Formatting.GOLD));
        return 1;
    }

    /**
     * Clear a large area of blocks around the given center position.
     * Creates a void pocket so the one block floats in open air.
     *
     * @param world       The server world
     * @param center      Center position (the one block location)
     * @param radius      Horizontal radius to clear (blocks in X/Z directions)
     * @param clearBelow  How many blocks below center to clear
     * @param clearAbove  How many blocks above center to clear
     */
    private static void clearArea(ServerWorld world, BlockPos center,
                                   int radius, int clearBelow, int clearAbove) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -clearBelow; y <= clearAbove; y++) {
                    BlockPos pos = center.add(x, y, z);
                    if (!world.getBlockState(pos).isAir()) {
                        world.removeBlock(pos, false);
                    }
                }
            }
        }
    }
}
