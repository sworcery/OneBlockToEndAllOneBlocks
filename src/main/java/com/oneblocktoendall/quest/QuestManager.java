package com.oneblocktoendall.quest;

import com.oneblocktoendall.OneBlockMod;
import com.oneblocktoendall.phase.Phase;
import com.oneblocktoendall.phase.PhaseManager;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 * Checks quest progress for players by reading Minecraft's built-in stat system
 * and scanning inventories. No mixins or event hooks needed — just stat polling.
 *
 * Called every ~20 ticks (1 second) by OneBlockTickHandler.
 */
public class QuestManager {

    public static void init() {
        OneBlockMod.LOGGER.info("Quest manager initialized");
    }

    /**
     * Check all active quests for a player. Returns list of newly completed quest IDs.
     */
    public static List<String> checkQuests(ServerPlayerEntity player, PlayerProgress progress) {
        if (!progress.isStarted() || progress.getCurrentPhase() < 1) {
            return Collections.emptyList();
        }

        Phase phase = PhaseManager.getPhase(progress.getCurrentPhase());
        if (phase == null) return Collections.emptyList();

        List<String> newlyCompleted = new ArrayList<>();

        for (Quest quest : phase.quests()) {
            if (progress.isQuestCompleted(quest.id())) continue;

            // Initialize baseline if this is the first check for this quest
            if (!progress.hasBaseline(quest.id())) {
                int baseline = getCurrentStatValue(player, quest);
                progress.setBaseline(quest.id(), baseline);
            }

            int currentValue = getCurrentStatValue(player, quest);
            int progressValue = currentValue - progress.getBaseline(quest.id());

            // For OBTAIN_ITEM, we use absolute inventory count (no baseline)
            if (quest.type() == QuestType.OBTAIN_ITEM) {
                progressValue = countItemInInventory(player, quest.target());
            }

            if (progressValue >= quest.count()) {
                progress.completeQuest(quest.id());
                newlyCompleted.add(quest.id());
            }
        }

        return newlyCompleted;
    }

    /**
     * Get the current progress value for a quest (before subtracting baseline).
     * Returns 0 for OBTAIN_ITEM since that uses inventory scanning instead.
     *
     * IMPORTANT: For stats that use registry lookups, we must retrieve the actual
     * registered instance from the registry. In 1.21.4, the stat system's internal
     * reverse lookup uses reference equality, so freshly created Identifiers won't
     * match the registered ones. We use Registries.X.get(key) to get the exact
     * registered object, then pass that to getOrCreateStat().
     */
    public static int getCurrentStatValue(ServerPlayerEntity player, Quest quest) {
        try {
            return switch (quest.type()) {
                case CRAFT_ITEM -> {
                    Item item = Registries.ITEM.get(Identifier.of(quest.target()));
                    yield player.getStatHandler().getStat(Stats.CRAFTED.getOrCreateStat(item));
                }
                case MINE_BLOCK -> {
                    net.minecraft.block.Block block = Registries.BLOCK.get(Identifier.of(quest.target()));
                    yield player.getStatHandler().getStat(Stats.MINED.getOrCreateStat(block));
                }
                case KILL_ENTITY -> {
                    Optional<EntityType<?>> entityType = EntityType.get(quest.target());
                    yield entityType.map(type ->
                            player.getStatHandler().getStat(Stats.KILLED.getOrCreateStat(type))
                    ).orElse(0);
                }
                case CUSTOM_STAT -> {
                    // Must get the REGISTERED Identifier instance from the CUSTOM_STAT registry.
                    // Creating a new Identifier.of() produces a different object that fails
                    // the stat system's identity-based reverse lookup in 1.21.4.
                    Identifier statKey = Identifier.of("minecraft", quest.target());
                    Identifier registeredStat = Registries.CUSTOM_STAT.get(statKey);
                    if (registeredStat == null) {
                        OneBlockMod.LOGGER.warn("Custom stat not found in registry: {}", quest.target());
                        yield 0;
                    }
                    yield player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(registeredStat));
                }
                case OBTAIN_ITEM -> 0; // Handled separately via inventory scan
            };
        } catch (Exception e) {
            OneBlockMod.LOGGER.warn("Error reading stat for quest '{}': {}", quest.id(), e.getMessage());
            return 0;
        }
    }

    /**
     * Get quest progress as a value between 0 and quest.count().
     * Used for display purposes (progress bars, etc.).
     */
    public static int getQuestProgress(ServerPlayerEntity player, Quest quest, PlayerProgress progress) {
        if (progress.isQuestCompleted(quest.id())) {
            return quest.count();
        }

        if (quest.type() == QuestType.OBTAIN_ITEM) {
            return Math.min(countItemInInventory(player, quest.target()), quest.count());
        }

        int currentValue = getCurrentStatValue(player, quest);
        int baseline = progress.getBaseline(quest.id());
        return Math.min(currentValue - baseline, quest.count());
    }

    /**
     * Check if all quests in the current phase are completed.
     */
    public static boolean isPhaseComplete(PlayerProgress progress) {
        Phase phase = PhaseManager.getPhase(progress.getCurrentPhase());
        if (phase == null) return false;

        for (Quest quest : phase.quests()) {
            if (!progress.isQuestCompleted(quest.id())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Count how many of a specific item the player has in their inventory.
     */
    private static int countItemInInventory(ServerPlayerEntity player, String itemId) {
        Item targetItem = Registries.ITEM.get(Identifier.of(itemId));
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(targetItem)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
