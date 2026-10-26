package io.github.moshushimjs.htgconnect.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UploadRoadPayload(String dimension, String json) implements CustomPacketPayload {

    public static final int MAX_BYTES = 8_000_000;

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("htg_connect", "upload_road");

    public static final CustomPacketPayload.Type<UploadRoadPayload> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, UploadRoadPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, UploadRoadPayload::dimension,
                    ByteBufCodecs.stringUtf8(MAX_BYTES), UploadRoadPayload::json,
                    UploadRoadPayload::new);

    @Override
    public Type<UploadRoadPayload> type() {
        return TYPE;
    }
}
