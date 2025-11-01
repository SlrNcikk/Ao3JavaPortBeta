package JavaBeta;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SpotifyService {

    private static final String CLIENT_ID = "60427f7ee95a4f2894d82fc5658e11a0";
    private static final String CLIENT_SECRET = "c69f922f3ff143c2aaab7df97621a53b";
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String BASE_URL = "https://api.spotify.com";

    private static String accessToken = null;
    private static String refreshToken = null;
    private static String deviceId = null;
    private static long expiresAt = 0L;
    private static final HttpClient client = HttpClient.newHttpClient();

    // ----------------- Authentication -----------------
    public static synchronized void authenticate() throws IOException, InterruptedException {
        if (accessToken != null && System.currentTimeMillis() < expiresAt) return;
        startFullAuthenticationFlowInternal();
    }

    private static void startFullAuthenticationFlowInternal() throws IOException, InterruptedException {
        CompletableFuture<String> codeFuture = new CompletableFuture<>();

        // Dynamic port allocation
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();  // save actual port

        server.createContext("/callback", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String code = null;
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length > 1 && pair[0].equals("code")) {
                        code = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                        break;
                    }
                }
            }
            String response;
            if (code != null) {
                response = "<html><body><h1>Success!</h1></body></html>";
                exchange.sendResponseHeaders(200, response.length());
                codeFuture.complete(code);
            } else {
                response = "<html><body><h1>Error</h1></body></html>";
                exchange.sendResponseHeaders(400, response.length());
                codeFuture.completeExceptionally(new IOException("No code in callback"));
            }
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().close();
        });

        server.setExecutor(null);
        server.start();

        String redirectUri = "http://127.0.0.1:" + port + "/callback";
        String scopes = "user-read-playback-state user-modify-playback-state";
        String authUrl = AUTH_URL + "?" +
                "client_id=" + CLIENT_ID +
                "&response_type=code" +
                "&redirect_uri=" + java.net.URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&scope=" + java.net.URLEncoder.encode(scopes, StandardCharsets.UTF_8);

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI.create(authUrl));
        } else {
            System.out.println("Open this URL: " + authUrl);
        }

        try {
            String code = codeFuture.get();
            server.stop(0);
            exchangeCodeForTokens(code, redirectUri);
        } catch (Exception e) {
            server.stop(0);
            throw new IOException("Failed to get authorization code", e);
        }
    }

    private static void exchangeCodeForTokens(String code, String redirectUri) throws IOException, InterruptedException {
        String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
        String encodedAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
        Map<Object, Object> data = new HashMap<>();
        data.put("grant_type", "authorization_code");
        data.put("code", code);
        data.put("redirect_uri", redirectUri);

        StringBuilder formBody = new StringBuilder();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            if (formBody.length() > 0) formBody.append("&");
            formBody.append(java.net.URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8));
            formBody.append("=");
            formBody.append(java.net.URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Authorization", encodedAuth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("Failed to get tokens: " + response.body());
        parseAndStoreTokens(response.body());
    }

    private static void parseAndStoreTokens(String responseBody) {
        JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
        accessToken = root.get("access_token").getAsString();
        if (root.has("refresh_token")) refreshToken = root.get("refresh_token").getAsString();
        int expiresIn = root.get("expires_in").getAsInt();
        expiresAt = System.currentTimeMillis() + (expiresIn * 1000L) - 5000L;
    }

    private static HttpRequest.Builder authRequest(String url) throws IOException, InterruptedException {
        if (accessToken == null) authenticate();
        if (System.currentTimeMillis() > expiresAt) refreshAccessToken();
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json");
    }

    private static boolean refreshAccessToken() throws IOException, InterruptedException {
        if (refreshToken == null) return false;
        String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
        String encodedAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
        Map<Object, Object> data = new HashMap<>();
        data.put("grant_type", "refresh_token");
        data.put("refresh_token", refreshToken);

        StringBuilder formBody = new StringBuilder();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            if (formBody.length() > 0) formBody.append("&");
            formBody.append(java.net.URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8));
            formBody.append("=");
            formBody.append(java.net.URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Authorization", encodedAuth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            parseAndStoreTokens(response.body());
            return true;
        } else {
            refreshToken = null;
            return false;
        }
    }

    // ----------------- Device -----------------
    public static void findAndSetDeviceId() throws IOException, InterruptedException {
        HttpRequest request = authRequest(BASE_URL + "/v1/me/player/devices").GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray devices = root.getAsJsonArray("devices");
        if (devices == null || devices.size() == 0) throw new IOException("No active Spotify devices found.");
        deviceId = devices.get(0).getAsJsonObject().get("id").getAsString();
    }

    private static void ensureDevice() throws IOException, InterruptedException {
        if (deviceId == null) findAndSetDeviceId();
    }

    private static void sendRequest(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            if (response.statusCode() == 401) {
                accessToken = null;
                expiresAt = 0L;
            }
            throw new IOException("Spotify API error: " + response.body());
        }
    }

    // ----------------- Player Methods -----------------
    public static void play() throws IOException, InterruptedException { ensureDevice(); sendRequest(authRequest(BASE_URL + "/v1/me/player/play?device_id=" + deviceId).PUT(HttpRequest.BodyPublishers.ofString("{}")).build()); }
    public static void pause() throws IOException, InterruptedException { ensureDevice(); sendRequest(authRequest(BASE_URL + "/v1/me/player/pause?device_id=" + deviceId).PUT(HttpRequest.BodyPublishers.ofString("{}")).build()); }
    public static void nextTrack() throws IOException, InterruptedException { ensureDevice(); sendRequest(authRequest(BASE_URL + "/v1/me/player/next?device_id=" + deviceId).POST(HttpRequest.BodyPublishers.ofString("{}")).build()); }
    public static void previousTrack() throws IOException, InterruptedException { ensureDevice(); sendRequest(authRequest(BASE_URL + "/v1/me/player/previous?device_id=" + deviceId).POST(HttpRequest.BodyPublishers.ofString("{}")).build()); }
    public static void playTrack(String uri) throws IOException, InterruptedException {
        ensureDevice();
        String body = uri.startsWith("spotify:track:") ? "{ \"uris\": [\"" + uri + "\"] }" : "{ \"context_uri\": \"" + uri + "\" }";
        sendRequest(authRequest(BASE_URL + "/v1/me/player/play?device_id=" + deviceId).PUT(HttpRequest.BodyPublishers.ofString(body)).build());
    }

    // ----------------- Search -----------------
    public static Map<String, String> searchAll(String query, int limit) throws IOException, InterruptedException {
        String encodedQuery = java.net.URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = BASE_URL + "/v1/search?q=" + encodedQuery + "&type=track,album,playlist&limit=" + limit;
        HttpRequest request = authRequest(url).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        Map<String, String> results = new LinkedHashMap<>();

        if (root.has("tracks")) {
            JsonArray items = root.getAsJsonObject("tracks").getAsJsonArray("items");
            for (var item : items) {
                JsonObject track = item.getAsJsonObject();
                results.put(track.get("name").getAsString(), track.get("uri").getAsString());
            }
        }
        if (root.has("albums")) {
            JsonArray items = root.getAsJsonObject("albums").getAsJsonArray("items");
            for (var item : items) {
                JsonObject album = item.getAsJsonObject();
                results.put(album.get("name").getAsString(), album.get("uri").getAsString());
            }
        }
        if (root.has("playlists")) {
            JsonArray items = root.getAsJsonObject("playlists").getAsJsonArray("items");
            for (var item : items) {
                JsonObject playlist = item.getAsJsonObject();
                results.put(playlist.get("name").getAsString(), playlist.get("uri").getAsString());
            }
        }
        return results;
    }

    // ----------------- Current Track -----------------
    public static JsonObject getCurrentTrackJson() throws IOException, InterruptedException {
        ensureDevice();
        String url = BASE_URL + "/v1/me/player/currently-playing?device_id=" + deviceId;
        HttpRequest request = authRequest(url).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) return null;
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}
