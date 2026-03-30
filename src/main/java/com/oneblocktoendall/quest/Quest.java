package com.oneblocktoendall.quest;

/**
 * Represents a single quest within a phase.
 *
 * @param id          Unique quest identifier (e.g., "survival_craft_table")
 * @param name        Display name (e.g., "First Steps")
 * @param description What the player needs to do (e.g., "Craft a crafting table")
 * @param type        How progress is tracked
 * @param target      The target identifier — item/block/entity/stat depending on type
 *                    (e.g., "minecraft:crafting_table", "minecraft:zombie", "animals_bred")
 * @param count       How many needed to complete the quest
 * @param phaseId     Which phase this quest belongs to
 */
public record Quest(
        String id,
        String name,
        String description,
        QuestType type,
        String target,
        int count,
        int phaseId
) {
}
