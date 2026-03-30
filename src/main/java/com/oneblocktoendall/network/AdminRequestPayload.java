package com.oneblocktoendall.network;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AdminRequestPayload() implements CustomPayload {

    public static final Id<AdminRequestPayload> ID = new Id<>(
            Identifier.of(OneBlockMod.MOD_ID, "admin_request"));

    public static final PacketCodec<RegistryByteBuf, AdminRequestPayload> CODEC =
            PacketCodec.of(AdminRequestPayload::write, AdminRequestPayload::read);

    private void write(RegistryByteBuf buf) {}
    private static AdminRequestPayload read(RegistryByteBuf buf) { return new AdminRequestPayload(); }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
