package com.oneblocktoendall.phase;

/**
 * A weighted mob spawn entry for a phase.
 *
 * @param entityId The entity type identifier (e.g., "minecraft:zombie")
 * @param weight   Relative weight for random selection among mob spawns
 */
public record MobSpawnEntry(String entityId, int weight) {
}
