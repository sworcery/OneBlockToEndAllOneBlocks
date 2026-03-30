package com.oneblocktoendall.network;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Network packet sent from server to client to sync quest progress.
 * The client uses this data to render the HUD overlay and quest book.
 *
 * Contains the current phase info and status of each quest (name, progress, completed).
 */
public record QuestSyncPayload(
        int currentPhase,
        int maxPhase,
        String phaseName,
        List<QuestStatus> quests
) implements CustomPayload {

    public static final Id<QuestSyncPayload> ID = new Id<>(
            Identifier.of(OneBlockMod.MOD_ID, "quest_sync"));

    /** Individual quest status sent to the client. */
    public record QuestStatus(
            String questId,
            String name,
            String description,
            int progress,
            int required,
            boolean completed
    ) {}

    /** Codec for serializing/deserializing the payload over the network. */
    public static final PacketCodec<RegistryByteBuf, QuestSyncPayload> CODEC =
            PacketCodec.of(QuestSyncPayload::write, QuestSyncPayload::read);

    private void write(RegistryByteBuf buf) {
        buf.writeInt(currentPhase);
        buf.writeInt(maxPhase);
        buf.writeString(phaseName);
        buf.writeInt(quests.size());
        for (QuestStatus quest : quests) {
            buf.writeString(quest.questId());
            buf.writeString(quest.name());
            buf.writeString(quest.description());
            buf.writeInt(quest.progress());
            buf.writeInt(quest.required());
            buf.writeBoolean(quest.completed());
        }
    }

    private static QuestSyncPayload read(RegistryByteBuf buf) {
        int phase = buf.readInt();
        int maxPhase = buf.readInt();
        String phaseName = buf.readString();
        int count = buf.readInt();
        List<QuestStatus> quests = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            quests.add(new QuestStatus(
                    buf.readString(),
                    buf.readString(),
                    buf.readString(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean()
            ));
        }
        return new QuestSyncPayload(phase, maxPhase, phaseName, quests);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
