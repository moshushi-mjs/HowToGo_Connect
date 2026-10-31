package io.github.moshushimjs.htgconnect.client;

import bili.dongsz.howtogo.api.WorldDataApi;
import io.github.moshushimjs.htgconnect.HowToGoConnect;
import io.github.moshushimjs.htgconnect.net.RequestRoadPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RoadSync {

    private static final int CHECK_INTERVAL_TICKS = 100;

    private static int knownRevision = -1;
    private static int tickCounter = 0;
    private static String lastDimension;

    private RoadSync() {
    }

    public static void markSynced() {
        knownRevision = WorldDataApi.roadRevision();
    }

    public static void reset() {
        knownRevision = -1;
        tickCounter = 0;
        lastDimension = null;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        String dimension = ClientRoads.currentDimension();
        if (dimension == null) {
            return;
        }

        if (!dimension.equals(lastDimension)) {
            lastDimension = dimension;
            knownRevision = -1;
            tickCounter = 0;
            if (ClientRoads.consented()) {
                PacketDistributor.sendToServer(
                        new RequestRoadPayload(dimension, ClientRoads.versionOf(dimension)));
            }
            return;
        }

        if (!ClientRoads.consented()) {
            return;
        }
        if (++tickCounter < CHECK_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        int revision = WorldDataApi.roadRevision();
        if (revision == knownRevision) {
            return;
        }
        if (knownRevision < 0) {
            knownRevision = revision;
            return;
        }

        knownRevision = revision;
        
        PacketDistributor.sendToServer(new RequestRoadPayload(dimension, -1));
    }
}
