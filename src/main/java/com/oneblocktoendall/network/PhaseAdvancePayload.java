package com.oneblocktoendall.network;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record PhaseAdvancePayload(
        int newPhase,
        String phaseName,
        List<String> newBlockNames,
        List<String> newMobNames,
        List<QuestSyncPayload.QuestStatus> newQuests
) implements CustomPayload {

    public static final Id<PhaseAdvancePayload> ID = new Id<>(
            Identifier.of(OneBlockMod.MOD_ID, "phase_advance"));

    public static final PacketCodec<RegistryByteBuf, PhaseAdvancePayload> CODEC =
            PacketCodec.of(PhaseAdvancePayload::write, PhaseAdvancePayload::read);

    private void write(RegistryByteBuf buf) {
        buf.writeInt(newPhase);
        buf.writeString(phaseName);
        buf.writeInt(newBlockNames.size());
        for (String s : newBlockNames) buf.writeString(s);
        buf.writeInt(newMobNames.size());
        for (String s : newMobNames) buf.writeString(s);
        buf.writeInt(newQuests.size());
        for (QuestSyncPayload.QuestStatus q : newQuests) {
            buf.writeString(q.questId());
            buf.writeString(q.name());
            buf.writeString(q.description());
            buf.writeInt(q.progress());
            buf.writeInt(q.required());
            buf.writeBoolean(q.completed());
        }
    }

    private static PhaseAdvancePayload read(RegistryByteBuf buf) {
        int phase = buf.readInt();
        String name = buf.readString();
        int blockCount = buf.readInt();
        List<String> blocks = new ArrayList<>();
        for (int i = 0; i < blockCount; i++) blocks.add(buf.readString());
        int mobCount = buf.readInt();
        List<String> mobs = new ArrayList<>();
        for (int i = 0; i < mobCount; i++) mobs.add(buf.readString());
        int questCount = buf.readInt();
        List<QuestSyncPayload.QuestStatus> quests = new ArrayList<>();
        for (int i = 0; i < questCount; i++) {
            quests.add(new QuestSyncPayload.QuestStatus(
                    buf.readString(), buf.readString(), buf.readString(),
                    buf.readInt(), buf.readInt(), buf.readBoolean()));
        }
        return new PhaseAdvancePayload(phase, name, blocks, mobs, quests);
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
