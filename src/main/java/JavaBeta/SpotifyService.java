package JavaBeta;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SpotifyService {

    private static final String CLIENT_ID = "60427f7ee95a4f2894d82fc5658e11a0";
    private static final String CLIENT_SECRET = "c69f922f3ff143c2aaab7df97621a53b";
    private static final String REDIRECT_URI = "http://127.0.0.1:8888/callback";
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String BASE_URL = "https://api.spotify.com";

    private static String accessToken;
    private static String activeDeviceId;
    private static HttpServer server;

    private static final Map<String, String> lastSearchResults = new HashMap<>();

    // === AUTHENTICATION ===
    public static void authenticate() throws IOException {
        new Thread(() -> {
            try {
                server = HttpServer.create(new InetSocketAddress(8888), 0);
                server.createContext("/callback", new HttpHandler() {
                    @Override
                    public void handle(HttpExchange exchange) throws IOException {
                        String query = exchange.getRequestURI().getQuery();
                        String code = query.split("code=")[1].split("&")[0];
                        System.out.println("Authorization code: " + code);

                        String response = "<h1>Login successful!</h1><p>You can close this tab.</p>";
                        exchange.sendResponseHeaders(200, response.getBytes().length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(response.getBytes());
                        }

                        exchangeCodeForToken(code);
                        new Thread(() -> server.stop(1)).start();
                    }
                });

                server.start();
                System.out.println("Server started on port 8888...");

                String scope = "user-modify-playback-state user-read-playback-state playlist-read-private";
                String authUri = AUTH_URL + "?client_id=" + CLIENT_ID +
                        "&response_type=code&redirect_uri=" + REDIRECT_URI +
                        "&scope=" + scope.replace(" ", "%20");

                System.out.println("Please open this URL in your browser to log in:");
                System.out.println(authUri);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void exchangeCodeForToken(String code) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

            String form = "grant_type=authorization_code"
                    + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                    + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .header("Authorization", "Basic " + encodedCredentials)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();

            setAccessToken(obj.get("access_token").getAsString());
            System.out.println("Access token acquired!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // === SEARCH ===
    public static Map<String, String> searchAll(String query, int limit) throws IOException {
        Map<String, String> results = new LinkedHashMap<>();
        String url = "https://api.spotify.com/v1/search?q=" +
                URLEncoder.encode(query, StandardCharsets.UTF_8) +
                "&type=track,album,playlist&limit=" + limit;

        String response = sendSpotifyRequest("GET", url, null);
        JSONObject json = new JSONObject(response);

        if (json.has("tracks")) {
            JSONArray tracks = json.getJSONObject("tracks").getJSONArray("items");
            for (int i = 0; i < tracks.length(); i++) {
                JSONObject track = tracks.getJSONObject(i);
                String name = "🎵 " + track.getString("name") + " - " +
                        track.getJSONArray("artists").getJSONObject(0).getString("name");
                results.put(name, track.getString("uri"));
            }
        }

        if (json.has("albums")) {
            JSONArray albums = json.getJSONObject("albums").getJSONArray("items");
            for (int i = 0; i < albums.length(); i++) {
                JSONObject album = albums.getJSONObject(i);
                String name = "💿 " + album.getString("name") + " (Album)";
                results.put(name, album.getString("uri"));
            }
        }

        if (json.has("playlists")) {
            JSONArray playlists = json.getJSONObject("playlists").getJSONArray("items");
            for (int i = 0; i < playlists.length(); i++) {
                JSONObject playlist = playlists.getJSONObject(i);
                String name = "📜 " + playlist.getString("name") + " (Playlist)";
                results.put(name, playlist.getString("uri"));
            }
        }

        lastSearchResults.clear();
        lastSearchResults.putAll(results);
        return results;
    }

    // === PLAYBACK CONTROLS ===
    public static void play() throws IOException {
        ensureActiveDevice();
        sendSpotifyRequest("PUT", BASE_URL + "/v1/me/player/play?device_id=" + activeDeviceId, null);
    }

    public static void pause() throws IOException {
        ensureActiveDevice();
        sendSpotifyRequest("PUT", BASE_URL + "/v1/me/player/pause?device_id=" + activeDeviceId, null);
    }

    public static void nextTrack() throws IOException {
        ensureActiveDevice();
        sendSpotifyRequest("POST", BASE_URL + "/v1/me/player/next?device_id=" + activeDeviceId, null);
    }

    public static void previousTrack() throws IOException {
        ensureActiveDevice();
        sendSpotifyRequest("POST", BASE_URL + "/v1/me/player/previous?device_id=" + activeDeviceId, null);
    }

    public static void playTrack(String trackUri) throws IOException {
        ensureActiveDevice();
        String jsonBody = "{\"uris\": [\"" + trackUri + "\"]}";
        sendSpotifyRequest("PUT", BASE_URL + "/v1/me/player/play?device_id=" + activeDeviceId, jsonBody);
    }

    // === HELPER ===
    private static String sendSpotifyRequest(String method, String url, String jsonBody) throws IOException {
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json");

            switch (method.toUpperCase()) {
                case "POST" -> builder.POST(jsonBody == null ?
                        HttpRequest.BodyPublishers.noBody() :
                        HttpRequest.BodyPublishers.ofString(jsonBody));
                case "PUT" -> builder.PUT(jsonBody == null ?
                        HttpRequest.BodyPublishers.noBody() :
                        HttpRequest.BodyPublishers.ofString(jsonBody));
                case "DELETE" -> builder.DELETE();
                default -> builder.GET();
            }

            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return response.body();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    private static void ensureActiveDevice() throws IOException {
        if (activeDeviceId == null) {
            System.err.println("No active Spotify device found!");
            throw new IOException("No active device found");
        }
    }

    public static void setAccessToken(String token) {
        accessToken = token;
    }
}
