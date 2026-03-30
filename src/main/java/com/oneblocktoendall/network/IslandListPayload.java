package com.oneblocktoendall.network;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record IslandListPayload(List<IslandInfo> islands) implements CustomPayload {

    public record IslandInfo(UUID playerId, String playerName, int phase, boolean allowsVisitors, boolean online) {}

    public static final Id<IslandListPayload> ID = new Id<>(
            Identifier.of(OneBlockMod.MOD_ID, "island_list"));

    public static final PacketCodec<RegistryByteBuf, IslandListPayload> CODEC =
            PacketCodec.of(IslandListPayload::write, IslandListPayload::read);

    private void write(RegistryByteBuf buf) {
        buf.writeInt(islands.size());
        for (IslandInfo i : islands) {
            buf.writeUuid(i.playerId());
            buf.writeString(i.playerName());
            buf.writeInt(i.phase());
            buf.writeBoolean(i.allowsVisitors());
            buf.writeBoolean(i.online());
        }
    }

    private static IslandListPayload read(RegistryByteBuf buf) {
        int count = buf.readInt();
        List<IslandInfo> islands = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            islands.add(new IslandInfo(
                    buf.readUuid(), buf.readString(), buf.readInt(),
                    buf.readBoolean(), buf.readBoolean()));
        }
        return new IslandListPayload(islands);
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
