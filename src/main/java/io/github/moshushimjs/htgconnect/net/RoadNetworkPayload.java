package io.github.moshushimjs.htgconnect.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RoadNetworkPayload(String dimension, int version, String json) implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("htg_connect", "road_network");

    public static final CustomPacketPayload.Type<RoadNetworkPayload> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, RoadNetworkPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, RoadNetworkPayload::dimension,
                    ByteBufCodecs.VAR_INT, RoadNetworkPayload::version,
                    ByteBufCodecs.stringUtf8(8_000_000), RoadNetworkPayload::json,
                    RoadNetworkPayload::new);

    @Override
    public Type<RoadNetworkPayload> type() {
        return TYPE;
    }
}
