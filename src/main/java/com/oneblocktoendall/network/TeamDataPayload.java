package com.oneblocktoendall.network;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record TeamDataPayload(
        boolean hasTeam,
        String teamName,
        String leaderName,
        List<String> members,
        List<String> pendingInvites,
        boolean mergedIslands,
        String message
) implements CustomPayload {

    public static final Id<TeamDataPayload> ID = new Id<>(
            Identifier.of(OneBlockMod.MOD_ID, "team_data"));

    public static final PacketCodec<RegistryByteBuf, TeamDataPayload> CODEC =
            PacketCodec.of(TeamDataPayload::write, TeamDataPayload::read);

    private void write(RegistryByteBuf buf) {
        buf.writeBoolean(hasTeam);
        buf.writeString(teamName);
        buf.writeString(leaderName);
        buf.writeInt(members.size());
        for (String m : members) buf.writeString(m);
        buf.writeInt(pendingInvites.size());
        for (String p : pendingInvites) buf.writeString(p);
        buf.writeBoolean(mergedIslands);
        buf.writeString(message);
    }

    private static TeamDataPayload read(RegistryByteBuf buf) {
        boolean hasTeam = buf.readBoolean();
        String name = buf.readString();
        String leader = buf.readString();
        int mCount = buf.readInt();
        List<String> members = new ArrayList<>();
        for (int i = 0; i < mCount; i++) members.add(buf.readString());
        int pCount = buf.readInt();
        List<String> pending = new ArrayList<>();
        for (int i = 0; i < pCount; i++) pending.add(buf.readString());
        return new TeamDataPayload(hasTeam, name, leader, members, pending, buf.readBoolean(), buf.readString());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
