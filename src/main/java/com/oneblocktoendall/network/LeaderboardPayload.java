package com.oneblocktoendall.network;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record LeaderboardPayload(
        List<LeaderboardEntry> entries,
        String requestingPlayer
) implements CustomPayload {

    public record LeaderboardEntry(String name, int phase, int questsCompleted, int blocksBroken) {}

    public static final Id<LeaderboardPayload> ID = new Id<>(
            Identifier.of(OneBlockMod.MOD_ID, "leaderboard"));

    public static final PacketCodec<RegistryByteBuf, LeaderboardPayload> CODEC =
            PacketCodec.of(LeaderboardPayload::write, LeaderboardPayload::read);

    private void write(RegistryByteBuf buf) {
        buf.writeInt(entries.size());
        for (LeaderboardEntry e : entries) {
            buf.writeString(e.name());
            buf.writeInt(e.phase());
            buf.writeInt(e.questsCompleted());
            buf.writeInt(e.blocksBroken());
        }
        buf.writeString(requestingPlayer);
    }

    private static LeaderboardPayload read(RegistryByteBuf buf) {
        int count = buf.readInt();
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entries.add(new LeaderboardEntry(
                    buf.readString(), buf.readInt(), buf.readInt(), buf.readInt()));
        }
        return new LeaderboardPayload(entries, buf.readString());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
