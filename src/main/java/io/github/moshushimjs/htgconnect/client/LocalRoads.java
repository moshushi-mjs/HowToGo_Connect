package io.github.moshushimjs.htgconnect.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import bili.dongsz.howtogo.client.RoadStore;
import bili.dongsz.howtogo.client.WorldFiles;
import bili.dongsz.howtogo.road.RoadStorage;
import io.github.moshushimjs.htgconnect.HowToGoConnect;
import io.github.moshushimjs.htgconnect.net.ClientPayloadHooks;
import io.github.moshushimjs.htgconnect.net.Dimensions;

public final class LocalRoads implements ClientPayloadHooks.LocalRoadsProvider {

    private static final String PLACES_FILE = "place_details.json";

    public LocalRoads() {
    }

    @Override
    public List<String> dimensions() {
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        String current = ClientRoads.currentDimension();
        if (current != null) {
            String stem = Dimensions.fileStem(current);
            result.add(stem);
            seen.add(stem);
        }

        Path dir = worldDirectory();
        if (dir == null || !Files.isDirectory(dir)) {
            return result;
        }

        try (Stream<Path> listing = Files.list(dir)) {
            for (Path path : listing.sorted().toList()) {
                String name = path.getFileName().toString();
                if (!name.endsWith(".json") || PLACES_FILE.equals(name)) {
                    continue;
                }
                if (name.indexOf('.') != name.length() - 5) {
                    continue;
                }
                String stem = name.substring(0, name.length() - 5);
                if (seen.add(stem)) {
                    result.add(stem);
                }
            }
        } catch (IOException e) {
            HowToGoConnect.LOGGER.warn("房主路网目录读取失败：{}", e.toString());
        }
        return result;
    }

    @Override
    public long stamp(String dimension) {
        String current = ClientRoads.currentDimension();
        if (current != null && Dimensions.same(current, dimension)) {
            return 1_000_000L + RoadStore.get().revision();
        }
        Path file = fileOf(dimension);
        if (file == null || !Files.isRegularFile(file)) {
            return -1L;
        }
        try {
            return Files.getLastModifiedTime(file).toMillis() * 31L + Files.size(file);
        } catch (IOException e) {
            return -1L;
        }
    }

    @Override
    public String text(String dimension) {
        String current = ClientRoads.currentDimension();
        if (current != null && Dimensions.same(current, dimension)) {
            return currentText();
        }
        Path file = fileOf(dimension);
        if (file == null || !Files.isRegularFile(file)) {
            return null;
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            HowToGoConnect.LOGGER.warn("房主路网文件读取失败（{}）：{}", dimension, e.toString());
            return null;
        }
    }

    private String currentText() {
        try {
            Path temp = Files.createTempFile("htg_connect_local_", ".json");
            try {
                if (!RoadStorage.save(temp, RoadStore.get())) {
                    return null;
                }
                return Files.readString(temp, StandardCharsets.UTF_8);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (Exception e) {
            HowToGoConnect.LOGGER.warn("读取房主本机路网失败：{}", e.toString());
            return null;
        }
    }

    private Path fileOf(String dimension) {
        Path dir = worldDirectory();
        if (dir == null) {
            return null;
        }
        return dir.resolve(Dimensions.fileStem(dimension) + ".json");
    }

    private Path worldDirectory() {
        try {
            return WorldFiles.of("").getParent();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
