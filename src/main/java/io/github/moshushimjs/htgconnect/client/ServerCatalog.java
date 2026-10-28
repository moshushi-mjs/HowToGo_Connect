package io.github.moshushimjs.htgconnect.client;

import io.github.moshushimjs.htgconnect.net.RoadOfferPayload;

import java.util.ArrayList;
import java.util.List;

public final class ServerCatalog {

    private static List<RoadOfferPayload.Entry> entries = List.of();
    private static long totalBytes = 0L;
    private static boolean received = false;

    private ServerCatalog() {
    }

    public static void update(List<RoadOfferPayload.Entry> incoming, long total) {
        entries = List.copyOf(new ArrayList<>(incoming));
        totalBytes = total;
        received = true;
    }

    public static void clear() {
        entries = List.of();
        totalBytes = 0L;
        received = false;
    }

    public static List<RoadOfferPayload.Entry> entries() {
        return entries;
    }

    public static long totalBytes() {
        return totalBytes;
    }

    public static boolean received() {
        return received;
    }

    public static boolean empty() {
        return entries.isEmpty();
    }
}
