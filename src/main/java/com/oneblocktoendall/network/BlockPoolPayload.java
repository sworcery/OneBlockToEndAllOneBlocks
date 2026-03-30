package com.oneblocktoendall.network;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record BlockPoolPayload(
        int phase,
        List<PoolEntry> blocks,
        List<PoolEntry> mobs,
        double mobSpawnChance
) implements CustomPayload {

    public record PoolEntry(String id, int weight) {}

    public static final Id<BlockPoolPayload> ID = new Id<>(
            Identifier.of(OneBlockMod.MOD_ID, "block_pool"));

    public static final PacketCodec<RegistryByteBuf, BlockPoolPayload> CODEC =
            PacketCodec.of(BlockPoolPayload::write, BlockPoolPayload::read);

    private void write(RegistryByteBuf buf) {
        buf.writeInt(phase);
        buf.writeInt(blocks.size());
        for (PoolEntry e : blocks) { buf.writeString(e.id()); buf.writeInt(e.weight()); }
        buf.writeInt(mobs.size());
        for (PoolEntry e : mobs) { buf.writeString(e.id()); buf.writeInt(e.weight()); }
        buf.writeDouble(mobSpawnChance);
    }

    private static BlockPoolPayload read(RegistryByteBuf buf) {
        int phase = buf.readInt();
        int bCount = buf.readInt();
        List<PoolEntry> blocks = new ArrayList<>();
        for (int i = 0; i < bCount; i++) blocks.add(new PoolEntry(buf.readString(), buf.readInt()));
        int mCount = buf.readInt();
        List<PoolEntry> mobs = new ArrayList<>();
        for (int i = 0; i < mCount; i++) mobs.add(new PoolEntry(buf.readString(), buf.readInt()));
        return new BlockPoolPayload(phase, blocks, mobs, buf.readDouble());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
