package io.github.moshushimjs.htgconnect.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestCatalogPayload() implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("htg_connect", "request_catalog");

    public static final CustomPacketPayload.Type<RequestCatalogPayload> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, RequestCatalogPayload> STREAM_CODEC =
            StreamCodec.unit(new RequestCatalogPayload());

    @Override
    public Type<RequestCatalogPayload> type() {
        return TYPE;
    }
}
