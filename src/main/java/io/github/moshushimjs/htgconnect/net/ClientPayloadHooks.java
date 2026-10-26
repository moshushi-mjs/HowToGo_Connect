package io.github.moshushimjs.htgconnect.net;

import java.util.List;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPayloadHooks {

    public interface RoadOfferHandler {
        void handle(RoadOfferPayload payload, IPayloadContext context);
    }

    public interface RoadNetworkHandler {
        void handle(RoadNetworkPayload payload, IPayloadContext context);
    }

    public interface LocalRoadsProvider {
        List<String> dimensions();

        long stamp(String dimension);

        String text(String dimension);
    }

    private static RoadOfferHandler roadOffer = (payload, context) -> {
    };
    private static RoadNetworkHandler roadNetwork = (payload, context) -> {
    };
    private static LocalRoadsProvider localRoads = new LocalRoadsProvider() {
        @Override
        public List<String> dimensions() {
            return List.of();
        }

        @Override
        public long stamp(String dimension) {
            return 0L;
        }

        @Override
        public String text(String dimension) {
            return null;
        }
    };

    private ClientPayloadHooks() {
    }

    public static void installRoadOffer(RoadOfferHandler handler) {
        roadOffer = handler;
    }

    public static void installRoadNetwork(RoadNetworkHandler handler) {
        roadNetwork = handler;
    }

    public static void installLocalRoads(LocalRoadsProvider provider) {
        localRoads = provider;
    }

    public static void onRoadOffer(RoadOfferPayload payload, IPayloadContext context) {
        roadOffer.handle(payload, context);
    }

    public static void onRoadNetwork(RoadNetworkPayload payload, IPayloadContext context) {
        roadNetwork.handle(payload, context);
    }

    public static List<String> localRoadDimensions() {
        return localRoads.dimensions();
    }

    public static long localRoadStamp(String dimension) {
        return localRoads.stamp(dimension);
    }

    public static String localRoadText(String dimension) {
        return localRoads.text(dimension);
    }
}
