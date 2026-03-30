package com.oneblocktoendall.network;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record AdminDataPayload(
        List<PlayerInfo> players,
        boolean autoStart
) implements CustomPayload {

    public record PlayerInfo(String name, int phase, int questsCompleted, int blocksBroken, boolean online, boolean spectating) {}

    public static final Id<AdminDataPayload> ID = new Id<>(
            Identifier.of(OneBlockMod.MOD_ID, "admin_data"));

    public static final PacketCodec<RegistryByteBuf, AdminDataPayload> CODEC =
            PacketCodec.of(AdminDataPayload::write, AdminDataPayload::read);

    private void write(RegistryByteBuf buf) {
        buf.writeInt(players.size());
        for (PlayerInfo p : players) {
            buf.writeString(p.name());
            buf.writeInt(p.phase());
            buf.writeInt(p.questsCompleted());
            buf.writeInt(p.blocksBroken());
            buf.writeBoolean(p.online());
            buf.writeBoolean(p.spectating());
        }
        buf.writeBoolean(autoStart);
    }

    private static AdminDataPayload read(RegistryByteBuf buf) {
        int count = buf.readInt();
        List<PlayerInfo> players = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            players.add(new PlayerInfo(
                    buf.readString(), buf.readInt(), buf.readInt(),
                    buf.readInt(), buf.readBoolean(), buf.readBoolean()));
        }
        return new AdminDataPayload(players, buf.readBoolean());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
