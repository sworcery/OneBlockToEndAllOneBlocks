package com.oneblocktoendall.network;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record IslandListRequestPayload() implements CustomPayload {

    public static final Id<IslandListRequestPayload> ID = new Id<>(
            Identifier.of(OneBlockMod.MOD_ID, "island_list_request"));

    public static final PacketCodec<RegistryByteBuf, IslandListRequestPayload> CODEC =
            PacketCodec.of(IslandListRequestPayload::write, IslandListRequestPayload::read);

    private void write(RegistryByteBuf buf) {}
    private static IslandListRequestPayload read(RegistryByteBuf buf) { return new IslandListRequestPayload(); }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
