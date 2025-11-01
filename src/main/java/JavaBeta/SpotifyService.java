package JavaBeta;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.io.entity.StringEntity;
import com.sun.net.httpserver.HttpHandler;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class SpotifyService {
    private static final String CLIENT_ID = "60427f7ee95a4f2894d82fc5658e11a0";
    private static final String CLIENT_SECRET = "c69f922f3ff143c2aaab7df97621a53b";
    private static final String REDIRECT_URI = "http://127.0.0.1:8888/callback";
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";

    private static String accessToken;
    private static HttpServer server;
    private static String activeDeviceId;

    public static String getActiveDeviceId() {
        return activeDeviceId;
    }

    public static void authenticate() throws IOException {
        new Thread(() -> {
            try {
                server = HttpServer.create(new InetSocketAddress(8888), 0);

                server.createContext("/callback", new HttpHandler() {
                    @Override
                    public void handle(HttpExchange exchange) throws IOException {
                        String query = exchange.getRequestURI().getQuery();
                        String code = query.split("code=")[1].split("&")[0];
                        System.out.println("Caught authorization code: " + code);

                        String response = "<h1>Success!</h1><p>You can close this tab and return to the app.</p>";
                        exchange.sendResponseHeaders(200, response.getBytes().length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();

                        exchangeCodeForToken(code);
                        new Thread(() -> server.stop(1)).start();
                    }
                });

                server.setExecutor(null);
                server.start();
                System.out.println("Local server started on port 8888...");

            } catch (IOException e) {
                System.err.println("Server start failed (port 8888 may be in use)");
                e.printStackTrace();
            }
        }).start();

        String scope = "user-modify-playback-state%20user-read-playback-state";
        String authUri = AUTH_URL + "?client_id=" + CLIENT_ID +
                "&response_type=code&redirect_uri=" + REDIRECT_URI +
                "&scope=" + scope;

        Desktop.getDesktop().browse(URI.create(authUri));
    }

    private static void exchangeCodeForToken(String code) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(TOKEN_URL);

            String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
            post.setHeader("Authorization", "Basic " + encodedCredentials);
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");

            List<NameValuePair> form = new ArrayList<>();
            form.add(new BasicNameValuePair("grant_type", "authorization_code"));
            form.add(new BasicNameValuePair("code", code));
            form.add(new BasicNameValuePair("redirect_uri", REDIRECT_URI));
            post.setEntity(new UrlEncodedFormEntity(form));

            String json = client.execute(post, response -> EntityUtils.toString(response.getEntity()));
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String token = obj.get("access_token").getAsString();
            setAccessToken(token);
            System.out.println("Access Token Acquired!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String searchTrack(String query) throws IOException {
        if (accessToken == null) return null;

        String encodedQuery = java.net.URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://api.spotify.com/v1/search?q=" + encodedQuery + "&type=track&limit=1";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(url);
            get.setHeader("Authorization", "Bearer " + accessToken);

            return client.execute(get, response -> {
                if (response.getCode() != 200) return null;

                String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                if (obj.has("tracks") && obj.getAsJsonObject("tracks").getAsJsonArray("items").size() > 0) {
                    JsonObject track = obj.getAsJsonObject("tracks")
                            .getAsJsonArray("items")
                            .get(0).getAsJsonObject();
                    return track.get("uri").getAsString(); // Spotify track URI
                } else {
                    return null;
                }
            });
        }
    }

    // Play a specific track by Spotify URI
    public static void playTrackByUri(String trackUri) throws IOException {
        ensureActiveDevice();
        String url = "https://api.spotify.com/v1/me/player/play?device_id=" + activeDeviceId;
        String jsonBody = "{ \"uris\": [\"" + trackUri + "\"] }";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPut put = new HttpPut(url);
            put.setHeader("Authorization", "Bearer " + accessToken);
            put.setHeader("Content-Type", "application/json");
            put.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

            client.execute(put, response -> null);
        }
    }

    public static void setAccessToken(String token) {
        accessToken = token;
    }

    private static void sendPutRequest(String url) throws IOException {
        if (accessToken == null || accessToken.isEmpty()) {
            System.err.println("No access token, cannot send PUT.");
            return;
        }
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPut put = new HttpPut(url);
            put.setHeader("Authorization", "Bearer " + accessToken);
            put.setEntity(new StringEntity("", StandardCharsets.UTF_8));
            client.execute(put, response -> null);
        }
    }

    private static void sendPostRequest(String url) throws IOException {
        if (accessToken == null || accessToken.isEmpty()) {
            System.err.println("No access token, cannot send POST.");
            return;
        }
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", "Bearer " + accessToken);
            post.setEntity(new StringEntity("", StandardCharsets.UTF_8));
            client.execute(post, response -> null);
        }
    }

    public static void play() throws IOException {
        ensureActiveDevice();
        sendPutRequest("https://api.spotify.com/v1/me/player/play?device_id=" + activeDeviceId);
    }

    public static void pause() throws IOException {
        ensureActiveDevice();
        sendPutRequest("https://api.spotify.com/v1/me/player/pause?device_id=" + activeDeviceId);
    }

    public static void nextTrack() throws IOException {
        ensureActiveDevice();
        sendPostRequest("https://api.spotify.com/v1/me/player/next?device_id=" + activeDeviceId);
    }

    public static void previousTrack() throws IOException {
        ensureActiveDevice();
        sendPostRequest("https://api.spotify.com/v1/me/player/previous?device_id=" + activeDeviceId);
    }

    private static void ensureActiveDevice() throws IOException {
        if (activeDeviceId == null) {
            findAndSetDeviceId();
            if (activeDeviceId == null) {
                throw new IOException("No active device found.");
            }
        }
    }

    public static void findAndSetDeviceId() throws IOException {
        if (accessToken == null) {
            System.err.println("Cannot find device, not logged in.");
            return;
        }
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet("https://api.spotify.com/v1/me/player/devices");
            get.setHeader("Authorization", "Bearer " + accessToken);

            String json = client.execute(get, response -> {
                if (response.getCode() != 200) return null;
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            });
            if (json == null) return;

            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("devices")) {
                for (var deviceElement : obj.getAsJsonArray("devices")) {
                    JsonObject device = deviceElement.getAsJsonObject();
                    if (device.has("is_active") && device.get("is_active").getAsBoolean()) {
                        activeDeviceId = device.get("id").getAsString();
                        return;
                    }
                }
                if (obj.getAsJsonArray("devices").size() > 0) {
                    activeDeviceId = obj.getAsJsonArray("devices").get(0)
                            .getAsJsonObject().get("id").getAsString();
                }
            }
        }
    }

    public static String getCurrentTrack() throws IOException {
        if (accessToken == null) return "Not logged in";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet("https://api.spotify.com/v1/me/player/currently-playing");
            get.setHeader("Authorization", "Bearer " + accessToken);

            return client.execute(get, response -> {
                if (response.getCode() == 204) return "Nothing playing";
                if (response.getCode() != 200) return "Error: " + response.getReasonPhrase();

                String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (json == null || json.isEmpty()) return "Nothing playing";

                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

                // Save active device from track info
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
                } else {
                    return "Nothing playing";
                }
            });
        }
    }
}
