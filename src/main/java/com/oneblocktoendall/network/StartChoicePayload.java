package com.oneblocktoendall.network;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StartChoicePayload(boolean startChallenge) implements CustomPayload {

    public static final Id<StartChoicePayload> ID = new Id<>(
            Identifier.of(OneBlockMod.MOD_ID, "start_choice"));

    public static final PacketCodec<RegistryByteBuf, StartChoicePayload> CODEC =
            PacketCodec.of(StartChoicePayload::write, StartChoicePayload::read);

    private void write(RegistryByteBuf buf) {
        buf.writeBoolean(startChallenge);
    }

    private static StartChoicePayload read(RegistryByteBuf buf) {
        return new StartChoicePayload(buf.readBoolean());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
