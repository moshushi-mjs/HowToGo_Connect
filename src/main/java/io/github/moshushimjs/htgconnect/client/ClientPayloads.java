package io.github.moshushimjs.htgconnect.client;

import java.util.List;

import io.github.moshushimjs.htgconnect.HowToGoConnect;
import io.github.moshushimjs.htgconnect.net.ClientPayloadHooks;
import io.github.moshushimjs.htgconnect.net.CatalogPayload;
import io.github.moshushimjs.htgconnect.net.Dimensions;
import io.github.moshushimjs.htgconnect.net.RequestRoadPayload;
import io.github.moshushimjs.htgconnect.net.RoadNetworkPayload;
import io.github.moshushimjs.htgconnect.net.RoadOfferPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPayloads {

    private ClientPayloads() {
    }

    public static void install() {
        ClientPayloadHooks.installRoadOffer(ClientPayloads::onRoadOffer);
        ClientPayloadHooks.installRoadNetwork(ClientPayloads::onRoadNetwork);
        ClientPayloadHooks.installCatalog(ClientPayloads::onCatalog);
        ClientPayloadHooks.installLocalRoads(new LocalRoads());
    }

    private static void onCatalog(CatalogPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ServerCatalog.update(payload.entries(), payload.totalBytes()));
    }

    private static void onRoadOffer(RoadOfferPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerCatalog.update(payload.entries(), payload.totalBytes());
            ServerCatalog.setKickOnDecline(payload.kickOnDecline());

            List<RoadOfferPayload.Entry> entries = payload.entries();
            if (entries.isEmpty()) {
                
                return;
            }
            if (Minecraft.getInstance().hasSingleplayerServer()) {
                
                return;
            }
            if (ClientRoads.consented()) {
                requestIfNeeded(entries);
                return;
            }
            if (ClientRoads.decided()) {
                
                return;
            }
            Minecraft.getInstance().setScreen(
                    new RoadConsentScreen(entries, payload.totalBytes(), payload.kickOnDecline()));
        });
    }

    private static void onRoadNetwork(RoadNetworkPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            String current = ClientRoads.currentDimension();
            if (current == null || !Dimensions.same(payload.dimension(), current)) {
                
                return;
            }
            if (!RoadImporter.importJson(payload.json())) {
                Messages.sendError("htg_connect.message.failed");
                return;
            }
            ClientRoads.setVersion(payload.dimension(), payload.version());

            if (Dimensions.isOverworld(payload.dimension())) {
                Messages.send("htg_connect.message.received");
            } else {
                Messages.send("htg_connect.message.received.dimension",
                        Dimensions.name(payload.dimension()));
            }
        });
    }

    private static void requestIfNeeded(List<RoadOfferPayload.Entry> entries) {
        String current = ClientRoads.currentDimension();
        if (current == null) {
            return;
        }
        int known = ClientRoads.versionOf(current);
        for (RoadOfferPayload.Entry entry : entries) {
            if (!Dimensions.same(entry.dimension(), current)) {
                continue;
            }
            if (entry.version() == known) {
                
                return;
            }
            PacketDistributor.sendToServer(new RequestRoadPayload(entry.dimension(), known));
            return;
        }
    }
}
