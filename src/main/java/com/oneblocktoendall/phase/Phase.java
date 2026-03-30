package com.oneblocktoendall.phase;

import com.oneblocktoendall.quest.Quest;

import java.util.List;

/**
 * Represents a progression phase in the one block challenge.
 * Each phase has its own block drop pool, mob spawns, and quests.
 *
 * @param id             Phase number (1-based)
 * @param name           Display name (e.g., "Survival", "Farming")
 * @param displayBlock   The block ID the one block appears as (e.g., "minecraft:grass_block")
 * @param blockPool      Weighted list of items that can drop when the one block is broken
 * @param mobSpawns      Weighted list of mobs that can spawn
 * @param mobSpawnChance Chance (0.0 to 1.0) of a mob spawning on each block break
 * @param quests         Quests that must be completed to advance to the next phase
 */
public record Phase(
        int id,
        String name,
        String displayBlock,
        List<BlockPoolEntry> blockPool,
        List<MobSpawnEntry> mobSpawns,
        double mobSpawnChance,
        List<Quest> quests
) {
}
