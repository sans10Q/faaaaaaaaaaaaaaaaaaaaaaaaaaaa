package com.lanterns;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client -> server: the ability key (default G) was pressed. The server decides what, if anything, it opens. */
public record AbilityKeyPayload() implements CustomPayload {
    public static final CustomPayload.Id<AbilityKeyPayload> ID = new CustomPayload.Id<>(Identifier.of(LanternsMod.MOD_ID, "ability_key"));
    public static final PacketCodec<RegistryByteBuf, AbilityKeyPayload> CODEC = PacketCodec.unit(new AbilityKeyPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
