package com.oneblocktoendall.network;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ToastPayload(
        int toastType,
        String playerName,
        String phaseName,
        String questName,
        int phaseNumber
) implements CustomPayload {

    public static final int TYPE_QUEST = 0;
    public static final int TYPE_PHASE = 1;
    public static final int TYPE_PHASE_BROADCAST = 2;

    public static final Id<ToastPayload> ID = new Id<>(
            Identifier.of(OneBlockMod.MOD_ID, "toast"));

    public static final PacketCodec<RegistryByteBuf, ToastPayload> CODEC =
            PacketCodec.of(ToastPayload::write, ToastPayload::read);

    private void write(RegistryByteBuf buf) {
        buf.writeInt(toastType);
        buf.writeString(playerName);
        buf.writeString(phaseName);
        buf.writeString(questName);
        buf.writeInt(phaseNumber);
    }

    private static ToastPayload read(RegistryByteBuf buf) {
        return new ToastPayload(
                buf.readInt(), buf.readString(), buf.readString(),
                buf.readString(), buf.readInt());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
