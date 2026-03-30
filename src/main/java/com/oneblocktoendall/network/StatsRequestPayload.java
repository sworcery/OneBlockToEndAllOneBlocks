package com.oneblocktoendall.network;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StatsRequestPayload() implements CustomPayload {

    public static final Id<StatsRequestPayload> ID = new Id<>(
            Identifier.of(OneBlockMod.MOD_ID, "stats_request"));

    public static final PacketCodec<RegistryByteBuf, StatsRequestPayload> CODEC =
            PacketCodec.of(StatsRequestPayload::write, StatsRequestPayload::read);

    private void write(RegistryByteBuf buf) {}
    private static StatsRequestPayload read(RegistryByteBuf buf) { return new StatsRequestPayload(); }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
