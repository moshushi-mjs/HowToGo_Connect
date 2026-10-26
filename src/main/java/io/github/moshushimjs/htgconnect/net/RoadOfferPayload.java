package io.github.moshushimjs.htgconnect.net;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RoadOfferPayload(List<Entry> entries, boolean kickOnDecline) implements CustomPacketPayload {

    public static final int MAX_ENTRIES = 256;

    public record Entry(String dimension, int version, int bytes) {

        public static final StreamCodec<FriendlyByteBuf, Entry> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, Entry::dimension,
                        ByteBufCodecs.VAR_INT, Entry::version,
                        ByteBufCodecs.VAR_INT, Entry::bytes,
                        Entry::new);
    }

    private static final StreamCodec<FriendlyByteBuf, List<Entry>> ENTRIES =
            ByteBufCodecs.collection(size -> new ArrayList<Entry>(size), Entry.STREAM_CODEC, MAX_ENTRIES);

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("htg_connect", "road_offer");

    public static final CustomPacketPayload.Type<RoadOfferPayload> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, RoadOfferPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ENTRIES, RoadOfferPayload::entries,
                    ByteBufCodecs.BOOL, RoadOfferPayload::kickOnDecline,
                    RoadOfferPayload::new);

    public long totalBytes() {
        long total = 0L;
        for (Entry entry : entries) {
            total += entry.bytes();
        }
        return total;
    }

    @Override
    public Type<RoadOfferPayload> type() {
        return TYPE;
    }
}
