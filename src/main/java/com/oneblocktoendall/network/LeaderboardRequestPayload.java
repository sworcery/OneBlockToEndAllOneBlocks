package com.oneblocktoendall.network;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record LeaderboardRequestPayload() implements CustomPayload {

    public static final Id<LeaderboardRequestPayload> ID = new Id<>(
            Identifier.of(OneBlockMod.MOD_ID, "leaderboard_request"));

    public static final PacketCodec<RegistryByteBuf, LeaderboardRequestPayload> CODEC =
            PacketCodec.of(LeaderboardRequestPayload::write, LeaderboardRequestPayload::read);

    private void write(RegistryByteBuf buf) {}
    private static LeaderboardRequestPayload read(RegistryByteBuf buf) { return new LeaderboardRequestPayload(); }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
