package com.oneblocktoendall.network;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record StatsPayload(
        int totalBlocksBroken,
        int totalQuestsCompleted,
        int currentPhase,
        int maxPhase,
        long challengeStartTime,
        long currentTime,
        List<PhaseStats> phaseStats
) implements CustomPayload {

    public record PhaseStats(int phase, String name, int questsDone, int questsTotal) {}

    public static final Id<StatsPayload> ID = new Id<>(
            Identifier.of(OneBlockMod.MOD_ID, "stats"));

    public static final PacketCodec<RegistryByteBuf, StatsPayload> CODEC =
            PacketCodec.of(StatsPayload::write, StatsPayload::read);

    private void write(RegistryByteBuf buf) {
        buf.writeInt(totalBlocksBroken);
        buf.writeInt(totalQuestsCompleted);
        buf.writeInt(currentPhase);
        buf.writeInt(maxPhase);
        buf.writeLong(challengeStartTime);
        buf.writeLong(currentTime);
        buf.writeInt(phaseStats.size());
        for (PhaseStats ps : phaseStats) {
            buf.writeInt(ps.phase());
            buf.writeString(ps.name());
            buf.writeInt(ps.questsDone());
            buf.writeInt(ps.questsTotal());
        }
    }

    private static StatsPayload read(RegistryByteBuf buf) {
        int blocks = buf.readInt();
        int quests = buf.readInt();
        int phase = buf.readInt();
        int max = buf.readInt();
        long start = buf.readLong();
        long current = buf.readLong();
        int count = buf.readInt();
        List<PhaseStats> stats = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            stats.add(new PhaseStats(buf.readInt(), buf.readString(), buf.readInt(), buf.readInt()));
        }
        return new StatsPayload(blocks, quests, phase, max, start, current, stats);
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
