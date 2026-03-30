package com.oneblocktoendall.network;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TeamActionPayload(String action, String target) implements CustomPayload {

    public static final String VIEW = "VIEW";
    public static final String CREATE = "CREATE";
    public static final String INVITE = "INVITE";
    public static final String ACCEPT = "ACCEPT";
    public static final String DECLINE = "DECLINE";
    public static final String LEAVE = "LEAVE";
    public static final String KICK = "KICK";

    public static final Id<TeamActionPayload> ID = new Id<>(
            Identifier.of(OneBlockMod.MOD_ID, "team_action"));

    public static final PacketCodec<RegistryByteBuf, TeamActionPayload> CODEC =
            PacketCodec.of(TeamActionPayload::write, TeamActionPayload::read);

    private void write(RegistryByteBuf buf) { buf.writeString(action); buf.writeString(target); }
    private static TeamActionPayload read(RegistryByteBuf buf) {
        return new TeamActionPayload(buf.readString(), buf.readString());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
