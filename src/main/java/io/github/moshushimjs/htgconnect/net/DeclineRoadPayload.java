package io.github.moshushimjs.htgconnect.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DeclineRoadPayload() implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("htg_connect", "decline_road");

    public static final CustomPacketPayload.Type<DeclineRoadPayload> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, DeclineRoadPayload> STREAM_CODEC =
            StreamCodec.unit(new DeclineRoadPayload());

    @Override
    public Type<DeclineRoadPayload> type() {
        return TYPE;
    }
}
