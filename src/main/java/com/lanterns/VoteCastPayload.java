package com.lanterns;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client -> server: a ballot. target = a player's UUID string, or "skip". The server validates everything. */
public record VoteCastPayload(String target) implements CustomPayload {
    public static final CustomPayload.Id<VoteCastPayload> ID = new CustomPayload.Id<>(Identifier.of(LanternsMod.MOD_ID, "vote_cast"));
    public static final PacketCodec<RegistryByteBuf, VoteCastPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, VoteCastPayload::target,
            VoteCastPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
