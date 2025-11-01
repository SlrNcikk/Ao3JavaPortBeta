package JavaBeta;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class SpotifyService {

    private static final String CLIENT_ID = "60427f7ee95a4f2894d82fc5658e11a0";
    private static final String CLIENT_SECRET = "c69f922f3ff143c2aaab7df97621a53b";
    private static final String REDIRECT_URI = "http://127.0.0.1:8888/callback";
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String BASE_URL = "https://api.spotify.com";

    private static String accessToken;
    private static String activeDeviceId;

    // === AUTHENTICATION ===
    public static void authenticate() throws IOException {
        try {
            // === Step 1: Spotify App Credentials ===

            // === Step 2: Create local server to handle the redirect ===
            com.sun.net.httpserver.HttpServer server =
                    com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(8888), 0);

            final String[] codeHolder = new String[1];

            server.createContext("/callback", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.contains("code=")) {
                    String code = query.substring(query.indexOf("code=") + 5);
                    if (code.contains("&")) {
                        code = code.substring(0, code.indexOf("&"));
                    }
                    codeHolder[0] = code;
                    String response = "<html><body><h1>Spotify login successful! You can close this window.</h1></body></html>";
                    exchange.sendResponseHeaders(200, response.length());
                    try (java.io.OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                }
            });

            server.start();

            // === Step 3: Open the Spotify authorization URL ===
            String scope = String.join(" ",
                    "user-read-playback-state",
                    "user-modify-playback-state",
                    "user-read-currently-playing",
                    "user-read-private",
                    "user-read-email"
            );

            String authUrl = AUTH_URL
                    + "?client_id=" + CLIENT_ID
                    + "&response_type=code"
                    + "&redirect_uri=" + java.net.URLEncoder.encode(REDIRECT_URI, "UTF-8")
                    + "&scope=" + java.net.URLEncoder.encode(scope, "UTF-8");

            java.awt.Desktop.getDesktop().browse(java.net.URI.create(authUrl));

            // === Step 4: Wait for the callback (user login) ===
            System.out.println("Waiting for Spotify login...");
            while (codeHolder[0] == null) {
                Thread.sleep(1000);
            }
            server.stop(0);

            // === Step 5: Exchange code for access token ===
            String tokenUrl = TOKEN_URL;

            String body = "grant_type=authorization_code"
                    + "&code=" + codeHolder[0]
                    + "&redirect_uri=" + java.net.URLEncoder.encode(REDIRECT_URI, "UTF-8")
                    + "&client_id=" + CLIENT_ID
                    + "&client_secret=" + CLIENT_SECRET;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            if (json.has("access_token")) {
                accessToken = json.get("access_token").getAsString();
                System.out.println("Spotify authenticated successfully.");
            } else {
                System.err.println("Failed to get access token: " + response.body());
            }

        } catch (Exception e) {
            throw new IOException("Spotify authentication failed: " + e.getMessage(), e);
        }
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
                case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody == null ? "" : jsonBody));
                case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(jsonBody == null ? "" : jsonBody));
                default -> builder.GET();
            }

            HttpRequest request = builder.build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            String body = response.body();

            // Handle common API errors
            if (status == 401) {
                System.err.println("Spotify API: Unauthorized (401) - token may be expired.");
                return null;
            } else if (status == 429) {
                System.err.println("Spotify API: Rate limit exceeded (429). Try again later.");
                return null;
            } else if (status >= 400) {
                System.err.println("Spotify API returned error " + status + ": " + body);
                return null;
            }

            if (body == null || body.isBlank()) {
                System.err.println("Spotify API returned empty response for " + url);
                return null;
            }

            return body;
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
        sendSpotifyRequest("PUT", url, null);
    }

    public static void pause() throws IOException {
        ensureActiveDevice();
        String url = BASE_URL + "/v1/me/player/pause?device_id=" + activeDeviceId;
        sendSpotifyRequest("PUT", url, null);
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

        String url = BASE_URL + "/v1/search?q=" +
                URLEncoder.encode(query, StandardCharsets.UTF_8) +
                "&type=track,album,playlist&limit=" + limit;

        String json = sendSpotifyRequest("GET", url, null);

        System.out.println("Raw Spotify response: " + json);

        if (json == null || json.isEmpty()) {
            System.err.println("Spotify search returned empty or invalid response for query: " + query);
            return results;
        }

        JsonElement element = JsonParser.parseString(json);
        if (!element.isJsonObject()) {
            System.err.println("Spotify search did not return a JSON object: " + json);
            return results;
        }
        JsonObject root = element.getAsJsonObject();


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
