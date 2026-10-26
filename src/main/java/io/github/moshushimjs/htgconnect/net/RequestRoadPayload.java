package io.github.moshushimjs.htgconnect.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestRoadPayload(String dimension, int knownVersion) implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("htg_connect", "request_road");

    public static final CustomPacketPayload.Type<RequestRoadPayload> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, RequestRoadPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, RequestRoadPayload::dimension,
                    ByteBufCodecs.VAR_INT, RequestRoadPayload::knownVersion,
                    RequestRoadPayload::new);

    @Override
    public Type<RequestRoadPayload> type() {
        return TYPE;
    }
}
