package com.oneblocktoendall.phase;

import com.google.gson.*;
import com.oneblocktoendall.OneBlockMod;
import com.oneblocktoendall.quest.Quest;
import com.oneblocktoendall.quest.QuestType;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Loads phase definitions from JSON and provides methods for:
 * - Getting a random drop from a phase's block pool
 * - Trying to spawn a mob on block break
 * - Looking up phase data by ID
 */
public class PhaseManager {

    private static final Map<Integer, Phase> PHASES = new LinkedHashMap<>();

    public static void init() {
        loadPhases();
        OneBlockMod.LOGGER.info("Loaded {} phases", PHASES.size());
    }

    public static Phase getPhase(int phaseId) {
        return PHASES.get(phaseId);
    }

    public static int getMaxPhase() {
        return PHASES.size();
    }

    public static Collection<Phase> getAllPhases() {
        return PHASES.values();
    }

    /**
     * Drop a random item from the given phase's block pool at the specified position.
     * Items from previous phases are included (cumulative pools).
     */
    public static void dropRandomItem(ServerWorld world, BlockPos pos, int currentPhase) {
        // Build cumulative pool from phase 1 through currentPhase
        List<BlockPoolEntry> cumulativePool = new ArrayList<>();
        for (int i = 1; i <= currentPhase; i++) {
            Phase phase = PHASES.get(i);
            if (phase != null) {
                cumulativePool.addAll(phase.blockPool());
            }
        }

        if (cumulativePool.isEmpty()) return;

        // Weighted random selection
        int totalWeight = cumulativePool.stream().mapToInt(BlockPoolEntry::weight).sum();
        if (totalWeight <= 0) return;
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int running = 0;

        for (BlockPoolEntry entry : cumulativePool) {
            running += entry.weight();
            if (roll < running) {
                Item item = Registries.ITEM.get(Identifier.of(entry.itemId()));
                if (item != null) {
                    Block.dropStack(world, pos.up(), new ItemStack(item));
                }
                return;
            }
        }
    }

    /**
     * Attempt to spawn a mob from the phase's mob pool.
     * Called each time the one block is broken.
     */
    public static void trySpawnMob(ServerWorld world, BlockPos pos, int currentPhase) {
        // Build cumulative mob pool
        List<MobSpawnEntry> cumulativeMobs = new ArrayList<>();
        double highestChance = 0;

        for (int i = 1; i <= currentPhase; i++) {
            Phase phase = PHASES.get(i);
            if (phase != null) {
                cumulativeMobs.addAll(phase.mobSpawns());
                highestChance = Math.max(highestChance, phase.mobSpawnChance());
            }
        }

        if (cumulativeMobs.isEmpty() || ThreadLocalRandom.current().nextDouble() >= highestChance) return;

        // Weighted random mob selection
        int totalWeight = cumulativeMobs.stream().mapToInt(MobSpawnEntry::weight).sum();
        if (totalWeight <= 0) return;
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int running = 0;

        for (MobSpawnEntry entry : cumulativeMobs) {
            running += entry.weight();
            if (roll < running) {
                Optional<EntityType<?>> optType = EntityType.get(entry.entityId());
                optType.ifPresent(type -> {
                    Entity entity = type.create(world, SpawnReason.EVENT);
                    if (entity != null) {
                        entity.refreshPositionAndAngles(
                                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                                ThreadLocalRandom.current().nextFloat() * 360f, 0f);
                        world.spawnEntity(entity);
                    }
                });
                return;
            }
        }
    }

    /**
     * Get item IDs that are new in this phase (not in any earlier phase's block pool).
     */
    public static List<String> getNewItemsForPhase(int phaseId) {
        Set<String> previousItems = new HashSet<>();
        for (int i = 1; i < phaseId; i++) {
            Phase p = PHASES.get(i);
            if (p != null) {
                for (BlockPoolEntry e : p.blockPool()) previousItems.add(e.itemId());
            }
        }
        List<String> newItems = new ArrayList<>();
        Phase current = PHASES.get(phaseId);
        if (current != null) {
            for (BlockPoolEntry e : current.blockPool()) {
                if (!previousItems.contains(e.itemId())) newItems.add(e.itemId());
            }
        }
        return newItems;
    }

    /**
     * Get entity IDs that are new in this phase (not in any earlier phase's mob pool).
     */
    public static List<String> getNewMobsForPhase(int phaseId) {
        Set<String> previousMobs = new HashSet<>();
        for (int i = 1; i < phaseId; i++) {
            Phase p = PHASES.get(i);
            if (p != null) {
                for (MobSpawnEntry e : p.mobSpawns()) previousMobs.add(e.entityId());
            }
        }
        List<String> newMobs = new ArrayList<>();
        Phase current = PHASES.get(phaseId);
        if (current != null) {
            for (MobSpawnEntry e : current.mobSpawns()) {
                if (!previousMobs.contains(e.entityId())) newMobs.add(e.entityId());
            }
        }
        return newMobs;
    }

    /**
     * Load all phase definitions from the bundled phases.json resource file.
     */
    private static void loadPhases() {
        try (InputStream is = PhaseManager.class.getResourceAsStream(
                "/data/oneblocktoendall/phases.json")) {
            if (is == null) {
                OneBlockMod.LOGGER.error("Could not find phases.json!");
                return;
            }

            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray phasesArray = root.getAsJsonArray("phases");

            for (JsonElement phaseElem : phasesArray) {
                JsonObject phaseObj = phaseElem.getAsJsonObject();
                int id = phaseObj.get("id").getAsInt();
                String name = phaseObj.get("name").getAsString();
                String displayBlock = phaseObj.get("displayBlock").getAsString();
                double mobSpawnChance = phaseObj.get("mobSpawnChance").getAsDouble();

                // Parse block pool
                List<BlockPoolEntry> blockPool = new ArrayList<>();
                for (JsonElement e : phaseObj.getAsJsonArray("blockPool")) {
                    JsonObject entry = e.getAsJsonObject();
                    blockPool.add(new BlockPoolEntry(
                            entry.get("item").getAsString(),
                            entry.get("weight").getAsInt()));
                }

                // Parse mob spawns
                List<MobSpawnEntry> mobSpawns = new ArrayList<>();
                for (JsonElement e : phaseObj.getAsJsonArray("mobSpawns")) {
                    JsonObject entry = e.getAsJsonObject();
                    mobSpawns.add(new MobSpawnEntry(
                            entry.get("entity").getAsString(),
                            entry.get("weight").getAsInt()));
                }

                // Parse quests
                List<Quest> quests = new ArrayList<>();
                for (JsonElement e : phaseObj.getAsJsonArray("quests")) {
                    JsonObject q = e.getAsJsonObject();
                    quests.add(new Quest(
                            q.get("id").getAsString(),
                            q.get("name").getAsString(),
                            q.get("description").getAsString(),
                            QuestType.valueOf(q.get("type").getAsString()),
                            q.get("target").getAsString(),
                            q.get("count").getAsInt(),
                            id));
                }

                PHASES.put(id, new Phase(id, name, displayBlock, blockPool, mobSpawns,
                        mobSpawnChance, quests));
            }
        } catch (Exception e) {
            OneBlockMod.LOGGER.error("Failed to load phases.json", e);
        }
    }
}
