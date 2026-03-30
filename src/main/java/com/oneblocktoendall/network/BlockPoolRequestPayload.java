package com.oneblocktoendall.network;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record BlockPoolRequestPayload(int requestedPhase) implements CustomPayload {

    public static final Id<BlockPoolRequestPayload> ID = new Id<>(
            Identifier.of(OneBlockMod.MOD_ID, "block_pool_request"));

    public static final PacketCodec<RegistryByteBuf, BlockPoolRequestPayload> CODEC =
            PacketCodec.of(BlockPoolRequestPayload::write, BlockPoolRequestPayload::read);

    private void write(RegistryByteBuf buf) { buf.writeInt(requestedPhase); }
    private static BlockPoolRequestPayload read(RegistryByteBuf buf) {
        return new BlockPoolRequestPayload(buf.readInt());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
