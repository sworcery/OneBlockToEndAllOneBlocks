package com.oneblocktoendall.network;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record QuestSyncPayload(
        int currentPhase,
        int maxPhase,
        String phaseName,
        List<QuestStatus> quests,
        List<QuestStatus> allianceQuests,
        List<QuestStatus> coopQuests,
        boolean isMergedTeam
) implements CustomPayload {

    public static final Id<QuestSyncPayload> ID = new Id<>(
            Identifier.of(OneBlockMod.MOD_ID, "quest_sync"));

    public record QuestStatus(
            String questId,
            String name,
            String description,
            int progress,
            int required,
            boolean completed
    ) {}

    public static final PacketCodec<RegistryByteBuf, QuestSyncPayload> CODEC =
            PacketCodec.of(QuestSyncPayload::write, QuestSyncPayload::read);

    private void write(RegistryByteBuf buf) {
        buf.writeInt(currentPhase);
        buf.writeInt(maxPhase);
        buf.writeString(phaseName);
        writeQuestList(buf, quests);
        writeQuestList(buf, allianceQuests);
        writeQuestList(buf, coopQuests);
        buf.writeBoolean(isMergedTeam);
    }

    private static QuestSyncPayload read(RegistryByteBuf buf) {
        int phase = buf.readInt();
        int maxPhase = buf.readInt();
        String phaseName = buf.readString();
        List<QuestStatus> quests = readQuestList(buf);
        List<QuestStatus> allianceQuests = readQuestList(buf);
        List<QuestStatus> coopQuests = readQuestList(buf);
        boolean isMergedTeam = buf.readBoolean();
        return new QuestSyncPayload(phase, maxPhase, phaseName,
                quests, allianceQuests, coopQuests, isMergedTeam);
    }

    private static void writeQuestList(RegistryByteBuf buf, List<QuestStatus> list) {
        buf.writeInt(list.size());
        for (QuestStatus quest : list) {
            buf.writeString(quest.questId());
            buf.writeString(quest.name());
            buf.writeString(quest.description());
            buf.writeInt(quest.progress());
            buf.writeInt(quest.required());
            buf.writeBoolean(quest.completed());
        }
    }

    private static List<QuestStatus> readQuestList(RegistryByteBuf buf) {
        int count = buf.readInt();
        List<QuestStatus> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new QuestStatus(
                    buf.readString(), buf.readString(), buf.readString(),
                    buf.readInt(), buf.readInt(), buf.readBoolean()));
        }
        return list;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
