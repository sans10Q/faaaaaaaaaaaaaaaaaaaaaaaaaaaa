package com.lanterns;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Server -> client: open a "pick a player" menu. data = "uuid|name;..." */
public record PickPlayerPayload(String purpose, String title, String data) implements CustomPayload {
    public static final CustomPayload.Id<PickPlayerPayload> ID = new CustomPayload.Id<>(Identifier.of(LanternsMod.MOD_ID, "pick_player"));
    public static final PacketCodec<RegistryByteBuf, PickPlayerPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, PickPlayerPayload::purpose,
            PacketCodecs.STRING, PickPlayerPayload::title,
            PacketCodecs.STRING, PickPlayerPayload::data,
            PickPlayerPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
