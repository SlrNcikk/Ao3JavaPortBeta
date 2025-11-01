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
// TODO: Add file imports when you implement saving/loading
// import java.nio.file.Files;
// import java.nio.file.Path;

public class SpotifyService {

    // --- Constants ---
    private static final String CLIENT_ID = "60427f7ee95a4f2894d82fc5658e11a0";
    private static final String CLIENT_SECRET = "c69f922f3ff143c2aaab7df97621a53b";
    private static final String REDIRECT_URI = "http://127.0.0.1:8888/callback";
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String BASE_URL = "https://api.spotify.com";

    // --- Fields ---
    private static String accessToken = null;
    private static String deviceId = null;
    private static String refreshToken = null;
    private static long expiresAt = 0L;
    private static final HttpClient client = HttpClient.newHttpClient();
    private static CompletableFuture<String> authCodeFuture;
    private static boolean isAuthenticating = false; // <-- The lock field

    // TODO: Define a path to save your refresh token
    // private static final Path TOKEN_FILE_PATH = Path.of("spotify_token.txt");

    // ----------------- Authentication -----------------

    /**
     * Main entry point for authentication.
     * Tries to load and refresh a token. If it fails, starts the full OAuth flow.
     */
    public static void authenticate() throws IOException, InterruptedException {
        // TODO: Uncomment this line when loadTokens() is implemented
        // if (loadTokens()) {
        //     System.out.println("Loaded refresh token from file.");
        //     if (refreshAccessToken()) {
        //         System.out.println("Spotify authenticated via refresh token.");
        //         return; // Success!
        //     }
        //     System.out.println("Could not refresh token. Requesting new one.");
        // }

        // If no token, or refresh failed, start the full browser flow.
        startFullAuthenticationFlow();
    }

    /**
     * Initiates the full OAuth 2.0 flow by opening a browser.
     */
    private static void startFullAuthenticationFlow() throws IOException, InterruptedException {

        // --- ✅ FIX ADDED HERE ---
        // This synchronized block acts as a "lock"
        synchronized (SpotifyService.class) {
            if (isAuthenticating) {
                System.out.println("Authentication already in progress. Ignoring new request.");
                return; // Exit, another thread is already handling this.
            }
            // We are now the one and only authenticating thread
            isAuthenticating = true;
        }
        // --- END OF FIX ---

        authCodeFuture = new CompletableFuture<>();

        // 1. Start a local server
        HttpServer server = startLocalServer();

        // 2. Build the authorization URL
        String scopes = "user-read-playback-state user-modify-playback-state";
        String authUrl = AUTH_URL + "?" +
                "client_id=" + CLIENT_ID +
                "&response_type=code" +
                "&redirect_uri=" + java.net.URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&scope=" + java.net.URLEncoder.encode(scopes, StandardCharsets.UTF_8);

        // 3. Open the URL in the user's default browser
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI.create(authUrl));
            System.out.println("Browser opened for authentication. Please log in.");
        } else {
            System.out.println("Please open this URL in your browser: " + authUrl);
        }

        // 4. Wait for the local server to receive the 'code'
        try {
            String code = authCodeFuture.get(); // This line blocks!
            System.out.println("Authorization code received.");

            // 5. Stop server and exchange the code for tokens.
            server.stop(0);
            exchangeCodeForTokens(code);

            // TODO: Uncomment this line when saveTokens() is implemented
            // saveTokens(); // Save the new refresh token

            isAuthenticating = false; // <-- ✅ FIX: Release the lock on success

        } catch (Exception e) {
            e.printStackTrace();
            server.stop(0);
            isAuthenticating = false; // <-- ✅ FIX: Release the lock on failure
            throw new IOException("Failed to get authorization code", e);
        }
    }

    /**
     * Starts a simple HTTP server to listen for the OAuth callback.
     */
    private static HttpServer startLocalServer() throws IOException {
        int port = URI.create(REDIRECT_URI).getPort();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/callback", (exchange) -> {
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
                response = "<html><body><h1>Success!</h1><p>You can close this tab and return to the application.</p></body></html>";
                exchange.sendResponseHeaders(200, response.length());
                authCodeFuture.complete(code); // Unblocks the authenticate() method
            } else {
                response = "<html><body><h1>Error</h1><p>No authorization code found. Please try again.</p></body></html>";
                exchange.sendResponseHeaders(400, response.length());
                authCodeFuture.completeExceptionally(new IOException("No code in callback"));
            }
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().close();
        });

        server.setExecutor(null);
        server.start();
        return server;
    }

    /**
     * Exchanges the authorization 'code' for an access token and refresh token.
     */
    private static void exchangeCodeForTokens(String code) throws IOException, InterruptedException {
        String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
        String encodedAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

        Map<Object, Object> data = new HashMap<>();
        data.put("grant_type", "authorization_code");
        data.put("code", code);
        data.put("redirect_uri", REDIRECT_URI);

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

        if (response.statusCode() != 200) {
            throw new IOException("Failed to get tokens: " + response.body());
        }

        parseAndStoreTokens(response.body());
        System.out.println("Spotify authenticated successfully!");
    }

    /**
     * Uses the stored 'refreshToken' to get a new 'accessToken'.
     * @return true if successful, false otherwise.
     */
    private static boolean refreshAccessToken() throws IOException, InterruptedException {
        if (refreshToken == null) {
            System.out.println("No refresh token available.");
            return false;
        }

        System.out.println("Refreshing access token...");
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
            System.out.println("Access token refreshed.");
            // TODO: If the response contains a *new* refresh token, save it
            // if (JsonParser.parseString(response.body()).getAsJsonObject().has("refresh_token")) {
            //    saveTokens();
            // }
            return true;
        } else {
            System.out.println("Failed to refresh token: " + response.body());
            refreshToken = null; // Token is likely invalid, force full re-auth
            return false;
        }
    }

    /**
     * Helper to parse the JSON response from Spotify and store token info.
     */
    private static void parseAndStoreTokens(String responseBody) {
        JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
        accessToken = root.get("access_token").getAsString();

        // Refresh token is not always included (e.g., during a refresh)
        if (root.has("refresh_token")) {
            refreshToken = root.get("refresh_token").getAsString();
        }

        int expiresIn = root.get("expires_in").getAsInt(); // Seconds
        expiresAt = System.currentTimeMillis() + (expiresIn * 1000L) - 5000L; // 5 sec buffer
    }

    /**
     * Checks if the token is expired and refreshes it if needed.
     */
    private static void checkAndRefreshToken() throws IOException, InterruptedException {
        if (System.currentTimeMillis() > expiresAt) {
            if (!refreshAccessToken()) {
                // If refresh fails, we must do the full auth flow again
                startFullAuthenticationFlow();
            }
        }
    }

    /**
     * Creates an authenticated HTTP request builder, refreshing the token if needed.
     */
    private static HttpRequest.Builder authRequest(String url) throws IOException, InterruptedException {
        if (accessToken == null) {
            // This is the first call, or auth failed.
            authenticate();
        }

        // Check if token is expired *before* making the call
        checkAndRefreshToken();

        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json");
    }

    // ----------------- Token Storage (TODO) -----------------

    /**
     * TODO: Implement this method to save the 'refreshToken' to a file.
     */
    // private static void saveTokens() {
    //     try {
    //         Files.writeString(TOKEN_FILE_PATH, refreshToken);
    //         System.out.println("Refresh token saved to file.");
    //     } catch (IOException e) {
    //         System.out.println("Error saving token: " + e.getMessage());
    //     }
    // }

    /**
     * TODO: Implement this method to load the 'refreshToken' from a file.
     * @return true if a token was loaded, false otherwise.
     */
    // private static boolean loadTokens() {
    //     try {
    //         if (Files.exists(TOKEN_FILE_PATH)) {
    //             refreshToken = Files.readString(TOKEN_FILE_PATH);
    //             return refreshToken != null && !refreshToken.isBlank();
    //         }
    //     } catch (IOException e) {
    //         System.out.println("Error loading token: " + e.getMessage());
    //     }
    //     return false;
    // }

    // ----------------- Device (No Changes) -----------------
    public static void findAndSetDeviceId() throws IOException, InterruptedException {
        HttpRequest request = authRequest(BASE_URL + "/v1/me/player/devices").GET().build(); // Now uses new authRequest()
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();

        if (!root.has("devices")) {
            throw new IOException("Spotify response missing 'devices' key. Check API/Auth.");
        }

        JsonArray devices = root.getAsJsonArray("devices");

        if (devices == null || devices.size() == 0) {
            throw new IOException("No active Spotify devices found! Make sure Spotify is open on one device.");
        }

        deviceId = devices.get(0).getAsJsonObject().get("id").getAsString();
        System.out.println("Using device ID: " + deviceId);
    }

    // ----------------- Player actions (No Changes) -----------------
    // All these methods will now use the new authRequest() automatically

    public static void play() throws IOException, InterruptedException {
        ensureDevice();
        String url = BASE_URL + "/v1/me/player/play?device_id=" + deviceId;
        HttpRequest request = authRequest(url).PUT(HttpRequest.BodyPublishers.ofString("{}")).build();
        sendRequest(request);
    }

    public static void pause() throws IOException, InterruptedException {
        ensureDevice();
        String url = BASE_URL + "/v1/me/player/pause?device_id=" + deviceId;
        HttpRequest request = authRequest(url).PUT(HttpRequest.BodyPublishers.ofString("{}")).build();
        sendRequest(request);
    }

    public static void nextTrack() throws IOException, InterruptedException {
        ensureDevice();
        String url = BASE_URL + "/v1/me/player/next?device_id=" + deviceId;
        HttpRequest request = authRequest(url).POST(HttpRequest.BodyPublishers.ofString("{}")).build();
        sendRequest(request);
    }

    public static void previousTrack() throws IOException, InterruptedException {
        ensureDevice();
        String url = BASE_URL + "/v1/me/player/previous?device_id=" + deviceId;
        HttpRequest request = authRequest(url).POST(HttpRequest.BodyPublishers.ofString("{}")).build();
        sendRequest(request);
    }

    public static void playTrack(String uri) throws IOException, InterruptedException {
        ensureDevice();
        String body;
        if (uri.startsWith("spotify:track:")) {
            body = "{ \"uris\": [\"" + uri + "\"] }"; // track
        } else {
            body = "{ \"context_uri\": \"" + uri + "\" }"; // album/playlist
        }
        String url = BASE_URL + "/v1/me/player/play?device_id=" + deviceId;
        HttpRequest request = authRequest(url).PUT(HttpRequest.BodyPublishers.ofString(body)).build();
        sendRequest(request);
    }

    // ----------------- Search (No Changes) -----------------
    public static Map<String, String> searchAll(String query, int limit) throws IOException, InterruptedException {
        String encodedQuery = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
        String url = BASE_URL + "/v1/search?q=" + encodedQuery
                + "&type=track,album,playlist&limit=" + limit;

        HttpRequest request = authRequest(url).GET().build(); // Now uses new authRequest()
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        Map<String, String> results = new LinkedHashMap<>();

        // Tracks
        if (root.has("tracks")) {
            JsonArray items = root.getAsJsonObject("tracks").getAsJsonArray("items");
            for (var item : items) {
                if (item != null && !item.isJsonNull()) {
                    JsonObject track = item.getAsJsonObject();
                    results.put(track.get("name").getAsString(), track.get("uri").getAsString());
                }
            }
        }
        // ... (rest of search method is unchanged) ...
        // Albums
        if (root.has("albums")) {
            JsonArray items = root.getAsJsonObject("albums").getAsJsonArray("items");
            for (var item : items) {
                if (item != null && !item.isJsonNull()) {
                    JsonObject album = item.getAsJsonObject();
                    results.put(album.get("name").getAsString(), album.get("uri").getAsString());
                }
            }
        }

        // Playlists
        if (root.has("playlists")) {
            JsonArray items = root.getAsJsonObject("playlists").getAsJsonArray("items");
            for (var item : items) {
                if (item != null && !item.isJsonNull()) {
                    JsonObject playlist = item.getAsJsonObject();
                    results.put(playlist.get("name").getAsString(), playlist.get("uri").getAsString());
                }
            }
        }

        return results;
    }

    // ----------------- Current track (No Changes) -----------------
    public static String getCurrentTrackJson() throws IOException, InterruptedException {
        ensureDevice();
        HttpRequest request = authRequest(BASE_URL + "/v1/me/player/currently-playing").GET().build(); // Now uses new authRequest()
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 204) return null; // no track playing
        if (response.statusCode() >= 400) throw new IOException("Spotify API error: " + response.body());

        return response.body();
    }

    // ----------------- Helpers (No Changes) -----------------
    private static void ensureDevice() throws IOException, InterruptedException {
        if (deviceId == null) findAndSetDeviceId();
    }

    private static void sendRequest(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            // If token is invalid (401), we should clear it to force re-auth
            if (response.statusCode() == 401) {
                System.out.println("Invalid access token (401). Forcing re-auth on next call.");
                accessToken = null;
                expiresAt = 0L;
            }
            throw new IOException("Spotify API error: " + response.body());
        }
    }
}