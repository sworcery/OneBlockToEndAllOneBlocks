package com.oneblocktoendall.network;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record TeleportRequestPayload(UUID targetPlayerId) implements CustomPayload {

    public static final Id<TeleportRequestPayload> ID = new Id<>(
            Identifier.of(OneBlockMod.MOD_ID, "teleport_request"));

    public static final PacketCodec<RegistryByteBuf, TeleportRequestPayload> CODEC =
            PacketCodec.of(TeleportRequestPayload::write, TeleportRequestPayload::read);

    private void write(RegistryByteBuf buf) { buf.writeUuid(targetPlayerId); }
    private static TeleportRequestPayload read(RegistryByteBuf buf) {
        return new TeleportRequestPayload(buf.readUuid());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
