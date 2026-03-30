package com.oneblocktoendall.quest;

/**
 * Types of quests the player can complete.
 * Each type maps to a different detection mechanism (stats, inventory, etc.)
 */
public enum QuestType {
    /** Craft X of a specific item. Tracked via Stats.CRAFTED. */
    CRAFT_ITEM,

    /** Mine X of a specific block. Tracked via Stats.MINED. */
    MINE_BLOCK,

    /** Kill X of a specific entity. Tracked via Stats.KILLED. */
    KILL_ENTITY,

    /** Have X of a specific item in inventory. Checked via inventory scan. */
    OBTAIN_ITEM,

    /** A generic Minecraft custom stat reaches a value (e.g., animals_bred). */
    CUSTOM_STAT
}
