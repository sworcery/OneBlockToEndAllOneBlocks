package com.oneblocktoendall.network;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TeleportResponsePayload(boolean success, String message) implements CustomPayload {

    public static final Id<TeleportResponsePayload> ID = new Id<>(
            Identifier.of(OneBlockMod.MOD_ID, "teleport_response"));

    public static final PacketCodec<RegistryByteBuf, TeleportResponsePayload> CODEC =
            PacketCodec.of(TeleportResponsePayload::write, TeleportResponsePayload::read);

    private void write(RegistryByteBuf buf) { buf.writeBoolean(success); buf.writeString(message); }
    private static TeleportResponsePayload read(RegistryByteBuf buf) {
        return new TeleportResponsePayload(buf.readBoolean(), buf.readString());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
