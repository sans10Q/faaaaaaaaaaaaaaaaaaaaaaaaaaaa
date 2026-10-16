package com.lanterns;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server -> client: state of the voting screen. data = "uuid|name|voted(0/1);..." for living players
 * (never WHO they voted for). Empty data closes the screen. open=true opens it, false only refreshes it.
 */
public record VoteStatePayload(String data, int secondsLeft, String myVote, boolean open) implements CustomPayload {
    public static final CustomPayload.Id<VoteStatePayload> ID = new CustomPayload.Id<>(Identifier.of(LanternsMod.MOD_ID, "vote_state"));
    public static final PacketCodec<RegistryByteBuf, VoteStatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, VoteStatePayload::data,
            PacketCodecs.VAR_INT, VoteStatePayload::secondsLeft,
            PacketCodecs.STRING, VoteStatePayload::myVote,
            PacketCodecs.BOOL, VoteStatePayload::open,
            VoteStatePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
