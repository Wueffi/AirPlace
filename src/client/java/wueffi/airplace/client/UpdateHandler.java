package wueffi.airplace.client;

import com.google.gson.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static wueffi.airplace.AirPlaceMain.LOGGER;

public class UpdateHandler {
    private static final String CURRENT_VERSION = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("airplace").get().getMetadata().getVersion().getFriendlyString();
    private static final String PROJECT_SLUG = "airplace";
    private static final String GAME_VERSION = net.minecraft.SharedConstants.getGameVersion().getName();
    private static final String LOADER = "fabric";

    private static String latestVersion = null;
    private static boolean checked = false;

    public static void initialize() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!checked) {
                checked = true;
                new Thread(() -> {
                    checkForUpdates();
                    client.execute(() -> {
                        if (client.player == null) return;

                        if (latestVersion == null) {
                            client.player.sendMessage(
                                    Text.literal("§cCould not find a fitting AirPlace Version! (Network Error)"),
                                    false
                            );
                            return;
                        }

                        int cmp = compareVersions(CURRENT_VERSION, latestVersion);
                        if (cmp < 0) {
                            client.player.sendMessage(
                                    Text.literal("§7[§6AirPlace§7]§a A new version of AirPlace is available: " + latestVersion +
                                                    " (you are on " + CURRENT_VERSION + ")")
                                            .formatted(Formatting.YELLOW),
                                    false
                            );
                        } else if (cmp == 0) {
                            client.player.sendMessage(
                                    Text.literal("§7[§6AirPlace§7]§a You are running the latest version of AirPlace (" + CURRENT_VERSION + ")")
                                            .formatted(Formatting.GREEN),
                                    false
                            );
                        } else {
                            client.player.sendMessage(
                                    Text.literal("§7[§6AirPlace§7]§a You are running a newer version of AirPlace (" + CURRENT_VERSION +
                                                    ") than the latest (" + latestVersion + ")")
                                            .formatted(Formatting.AQUA),
                                    false
                            );
                        }
                    });
                }).start();
            }
        });
    }

    private static void checkForUpdates() {
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.modrinth.com/v2/project/" + PROJECT_SLUG + "/version"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                LOGGER.warn("AirPlace update check: Project or versions not found (404).");
                return;
            } else if (response.statusCode() != 200) {
                LOGGER.error("AirPlace update check failed: HTTP " + response.statusCode());
                return;
            }
            JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();

            List<JsonObject> cleanedVersions = versions.asList().stream()
                    .map(JsonElement::getAsJsonObject)
                    .filter(v -> {
                        JsonArray games = v.getAsJsonArray("game_versions");
                        JsonArray loaders = v.getAsJsonArray("loaders");
                        return containsString(games, GAME_VERSION) && containsString(loaders, LOADER);
                    })
                    .toList();

            Optional<JsonObject> latest = cleanedVersions.stream()
                    .max(Comparator.comparing(v -> Instant.parse(v.get("date_published").getAsString())));

            latestVersion = latest.map(v -> v.get("version_number").getAsString())
                    .orElse(CURRENT_VERSION);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean containsString(JsonArray arr, String value) {
        for (JsonElement el : arr) {
            if (el.getAsString().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private static int compareVersions(String v1, String v2) {
        String[] a1 = v1.split("\\.");
        String[] a2 = v2.split("\\.");
        int len = Math.max(a1.length, a2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < a1.length ? parseIntSafe(a1[i]) : 0;
            int n2 = i < a2.length ? parseIntSafe(a2[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
