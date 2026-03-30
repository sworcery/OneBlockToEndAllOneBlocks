package com.oneblocktoendall.phase;

/**
 * A weighted entry in a phase's block/item drop pool.
 * Higher weight = more likely to drop when the one block is broken.
 *
 * @param itemId The item identifier (e.g., "minecraft:dirt", "minecraft:oak_log")
 * @param weight Relative weight for random selection
 */
public record BlockPoolEntry(String itemId, int weight) {
}
