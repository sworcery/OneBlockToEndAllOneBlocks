package com.oneblocktoendall.network;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record VisitorTogglePayload(boolean allowVisitors) implements CustomPayload {

    public static final Id<VisitorTogglePayload> ID = new Id<>(
            Identifier.of(OneBlockMod.MOD_ID, "visitor_toggle"));

    public static final PacketCodec<RegistryByteBuf, VisitorTogglePayload> CODEC =
            PacketCodec.of(VisitorTogglePayload::write, VisitorTogglePayload::read);

    private void write(RegistryByteBuf buf) { buf.writeBoolean(allowVisitors); }
    private static VisitorTogglePayload read(RegistryByteBuf buf) {
        return new VisitorTogglePayload(buf.readBoolean());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
