package com.lanterns;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client -> server: the player chose someone in a pick menu. The server re-validates everything. */
public record PickResultPayload(String purpose, String target) implements CustomPayload {
    public static final CustomPayload.Id<PickResultPayload> ID = new CustomPayload.Id<>(Identifier.of(LanternsMod.MOD_ID, "pick_result"));
    public static final PacketCodec<RegistryByteBuf, PickResultPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, PickResultPayload::purpose,
            PacketCodecs.STRING, PickResultPayload::target,
            PickResultPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
