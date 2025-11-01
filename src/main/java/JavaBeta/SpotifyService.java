package JavaBeta;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SpotifyService {

    private static final String BASE_URL = "https://api.spotify.com";
    private static String accessToken = null;
    private static String activeDeviceId = null;

    // === AUTHENTICATION ===
    public static void authenticate() throws IOException {
        // your existing OAuth/login logic here
        // make sure to set accessToken = "..." after obtaining it
    }

    // === GENERIC REQUEST HANDLER ===
    public static String sendSpotifyRequest(String method, String url, String jsonBody) throws IOException {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json");

            switch (method.toUpperCase()) {
                case "POST":
                    builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody == null ? "" : jsonBody));
                    break;
                case "PUT":
                    builder.PUT(HttpRequest.BodyPublishers.ofString(jsonBody == null ? "" : jsonBody));
                    break;
                default:
                    builder.GET();
                    break;
            }

            HttpRequest request = builder.build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            throw new IOException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    // === FIND DEVICE ===
    public static void findAndSetDeviceId() throws IOException {
        if (accessToken == null) {
            System.err.println("Cannot find device — not logged in.");
            return;
        }

        String url = BASE_URL + "/v1/me/player/devices";
        String json = sendSpotifyRequest("GET", url, null);
        if (json == null || json.isEmpty()) return;

        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        if (obj.has("devices")) {
            for (var element : obj.getAsJsonArray("devices")) {
                JsonObject device = element.getAsJsonObject();
                if (device.has("is_active") && device.get("is_active").getAsBoolean()) {
                    activeDeviceId = device.get("id").getAsString();
                    System.out.println("Active device: " + activeDeviceId);
                    return;
                }
            }
            if (!obj.getAsJsonArray("devices").isEmpty()) {
                activeDeviceId = obj.getAsJsonArray("devices")
                        .get(0).getAsJsonObject().get("id").getAsString();
                System.out.println("Using first available device: " + activeDeviceId);
            }
        }
    }

    // === GET CURRENT TRACK ===
    public static String getCurrentTrack() throws IOException {
        if (accessToken == null) return "Not logged in";

        String url = BASE_URL + "/v1/me/player/currently-playing";
        String json = sendSpotifyRequest("GET", url, null);
        if (json == null || json.isEmpty()) return "Nothing playing";

        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

        if (obj.has("device") && !obj.get("device").isJsonNull()) {
            JsonObject device = obj.getAsJsonObject("device");
            activeDeviceId = device.get("id").getAsString();
        }

        if (obj.has("item") && !obj.get("item").isJsonNull()) {
            JsonObject item = obj.getAsJsonObject("item");
            String name = item.get("name").getAsString();
            String artist = item.getAsJsonArray("artists").get(0)
                    .getAsJsonObject().get("name").getAsString();
            return name + " — " + artist;
        }

        return "Nothing playing";
    }

    // === PLAYER CONTROLS ===
    public static void play() throws IOException {
        ensureActiveDevice();
        String url = BASE_URL + "/v1/me/player/play?device_id=" + activeDeviceId;
        sendSpotifyRequest("PUT", url, "{}");
    }

    public static void pause() throws IOException {
        ensureActiveDevice();
        String url = BASE_URL + "/v1/me/player/pause?device_id=" + activeDeviceId;
        sendSpotifyRequest("PUT", url, "{}");
    }

    public static void nextTrack() throws IOException {
        ensureActiveDevice();
        String url = BASE_URL + "/v1/me/player/next?device_id=" + activeDeviceId;
        sendSpotifyRequest("POST", url, null);
    }

    public static void previousTrack() throws IOException {
        ensureActiveDevice();
        String url = BASE_URL + "/v1/me/player/previous?device_id=" + activeDeviceId;
        sendSpotifyRequest("POST", url, null);
    }

    public static void playTrack(String uri) throws IOException {
        ensureActiveDevice();
        String url = BASE_URL + "/v1/me/player/play?device_id=" + activeDeviceId;
        String body = "{ \"uris\": [\"" + uri + "\"] }";
        sendSpotifyRequest("PUT", url, body);
    }

    // === ENSURE DEVICE ===
    private static void ensureActiveDevice() throws IOException {
        if (activeDeviceId == null) findAndSetDeviceId();
    }

    // === SEARCH ===
    public static java.util.Map<String, String> searchAll(String query, int limit) throws IOException {
        java.util.Map<String, String> results = new java.util.LinkedHashMap<>();
        if (accessToken == null) throw new IOException("Not authenticated");

        String url = BASE_URL + "/v1/search?q=" + query.replace(" ", "%20")
                + "&type=track,album,playlist&limit=" + limit;

        String json = sendSpotifyRequest("GET", url, null);
        if (json == null || json.isEmpty()) return results;

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        if (root.has("tracks")) {
            for (var item : root.getAsJsonObject("tracks").getAsJsonArray("items")) {
                JsonObject track = item.getAsJsonObject();
                String name = track.get("name").getAsString();
                String artist = track.getAsJsonArray("artists").get(0)
                        .getAsJsonObject().get("name").getAsString();
                results.put("🎵 " + name + " — " + artist, track.get("uri").getAsString());
            }
        }
        if (root.has("albums")) {
            for (var item : root.getAsJsonObject("albums").getAsJsonArray("items")) {
                JsonObject album = item.getAsJsonObject();
                results.put("💿 " + album.get("name").getAsString(),
                        album.get("uri").getAsString());
            }
        }
        if (root.has("playlists")) {
            for (var item : root.getAsJsonObject("playlists").getAsJsonArray("items")) {
                JsonObject playlist = item.getAsJsonObject();
                results.put("📜 " + playlist.get("name").getAsString(),
                        playlist.get("uri").getAsString());
            }
        }

        return results;
    }
}
