package io.github.moshushimjs.htgconnect.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record CatalogPayload(List<RoadOfferPayload.Entry> entries) implements CustomPacketPayload {

    private static final StreamCodec<FriendlyByteBuf, List<RoadOfferPayload.Entry>> ENTRIES =
            ByteBufCodecs.collection(size -> new ArrayList<RoadOfferPayload.Entry>(size),
                    RoadOfferPayload.Entry.STREAM_CODEC, RoadOfferPayload.MAX_ENTRIES);

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("htg_connect", "catalog");

    public static final CustomPacketPayload.Type<CatalogPayload> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, CatalogPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ENTRIES, CatalogPayload::entries,
                    CatalogPayload::new);

    public long totalBytes() {
        long total = 0L;
        for (RoadOfferPayload.Entry entry : entries) {
            total += entry.bytes();
        }
        return total;
    }

    @Override
    public Type<CatalogPayload> type() {
        return TYPE;
    }
}
