package com.oneblocktoendall.network;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AdminActionPayload(String action, String targetPlayer, int value) implements CustomPayload {

    public static final String SET_PHASE = "SET_PHASE";
    public static final String RESET_PLAYER = "RESET_PLAYER";
    public static final String TOGGLE_AUTO_START = "TOGGLE_AUTO_START";

    public static final Id<AdminActionPayload> ID = new Id<>(
            Identifier.of(OneBlockMod.MOD_ID, "admin_action"));

    public static final PacketCodec<RegistryByteBuf, AdminActionPayload> CODEC =
            PacketCodec.of(AdminActionPayload::write, AdminActionPayload::read);

    private void write(RegistryByteBuf buf) {
        buf.writeString(action);
        buf.writeString(targetPlayer);
        buf.writeInt(value);
    }

    private static AdminActionPayload read(RegistryByteBuf buf) {
        return new AdminActionPayload(buf.readString(), buf.readString(), buf.readInt());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
