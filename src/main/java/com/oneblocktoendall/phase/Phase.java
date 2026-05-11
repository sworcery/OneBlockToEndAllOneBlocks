package com.oneblocktoendall.phase;

import com.oneblocktoendall.quest.Quest;

import java.util.List;

public record Phase(
        int id,
        String name,
        String displayBlock,
        List<BlockPoolEntry> blockPool,
        List<MobSpawnEntry> mobSpawns,
        double mobSpawnChance,
        List<Quest> quests,
        List<Quest> allianceQuests,
        List<Quest> coopQuests
) {
}
