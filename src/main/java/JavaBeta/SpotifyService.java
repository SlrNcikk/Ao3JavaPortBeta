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
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import com.sun.net.httpserver.HttpHandler;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.apache.hc.core5.http.io.entity.StringEntity;
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
        // Start a server on a new thread to listen for the callback
        new Thread(() -> {
            try {
                // --- This block is now safer ---
                server = HttpServer.create(new InetSocketAddress(8888), 0);

                // This is the "listener" using the stable HttpHandler class
                server.createContext("/callback", new HttpHandler() {
                    @Override
                    public void handle(HttpExchange exchange) throws IOException {
                        // --- 1. CATCH THE CODE ---
                        String query = exchange.getRequestURI().getQuery();
                        String code = query.split("code=")[1].split("&")[0];
                        System.out.println("Caught authorization code: " + code);

                        // --- 2. SEND A "SUCCESS" MESSAGE TO BROWSER ---
                        String response = "<h1>Success!</h1><p>You can close this tab and return to the app.</p>";
                        exchange.sendResponseHeaders(200, response.getBytes().length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();

                        // --- 3. EXCHANGE THE CODE FOR A TOKEN ---
                        exchangeCodeForToken(code);

                        // --- 4. STOP THE SERVER ---
                        new Thread(() -> server.stop(1)).start();
                    }
                });

                server.setExecutor(null); // creates a default executor
                server.start();
                System.out.println("Local server started on port 8888...");

            } catch (IOException e) {
                System.err.println("!!! Server start failed (port 8888 may be in use) !!!");
                e.printStackTrace();
            }
        }).start();

        // --- OPEN THE BROWSER FOR LOGIN ---

        // --- THIS IS THE FIX for Bug 1 ---
        // We replaced the space with "%20"
        String scope = "user-modify-playback-state%20user-read-playback-state";

        String authUri = AUTH_URL + "?client_id=" + CLIENT_ID +
                "&response_type=code&redirect_uri=" + REDIRECT_URI +
                "&scope=" + scope;

        Desktop.getDesktop().browse(URI.create(authUri));
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
                if (response.getCode() != 200) {
                    System.err.println("Failed to get devices: " + response.getReasonPhrase());
                    return null;
                }
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            });

            if (json == null) return;

            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("devices")) {
                // First try to find an active device
                for (var deviceElement : obj.getAsJsonArray("devices")) {
                    JsonObject device = deviceElement.getAsJsonObject();
                    if (device.has("is_active") && device.get("is_active").getAsBoolean()) {
                        activeDeviceId = device.get("id").getAsString();
                        System.out.println("DEBUG: Active Device ID set to: " + activeDeviceId);
                        return;
                    }
                }

                // If no active device, pick the first available device
                if (obj.getAsJsonArray("devices").size() > 0) {
                    JsonObject firstDevice = obj.getAsJsonArray("devices").get(0).getAsJsonObject();
                    activeDeviceId = firstDevice.get("id").getAsString();
                    System.out.println("DEBUG: No active device, selected first available device: " + activeDeviceId);
                    return;
                }
            }

            // If we get here, no devices found
            System.err.println("DEBUG: Could not find any Spotify devices.");
            activeDeviceId = null;
        }
    }

    private static void exchangeCodeForToken(String code) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(TOKEN_URL);

            // --- 1. SET HEADERS (Basic Auth) ---
            String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
            post.setHeader("Authorization", "Basic " + encodedCredentials);
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");

            // --- 2. SET BODY (Form data) ---
            List<NameValuePair> form = new ArrayList<>();
            form.add(new BasicNameValuePair("grant_type", "authorization_code"));
            form.add(new BasicNameValuePair("code", code));
            form.add(new BasicNameValuePair("redirect_uri", REDIRECT_URI));
            post.setEntity(new UrlEncodedFormEntity(form));

            // --- 3. EXECUTE REQUEST ---
            System.out.println("Exchanging code for token...");

            // Execute and get the response as a string
            String json = client.execute(post, response -> {
                return EntityUtils.toString(response.getEntity());
            });

            // --- 4. PARSE TOKEN AND SET IT ---
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String token = obj.get("access_token").getAsString();

            setAccessToken(token); // This is your existing method!
            System.out.println("Access Token Acquired!");

        } catch (Exception e) {
            e.printStackTrace();
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
            put.setEntity(new StringEntity("", StandardCharsets.UTF_8)); // <-- empty body
            client.execute(put, response -> {
                System.out.println("PUT Response: " + response.getCode() + " " + response.getReasonPhrase());
                return null;
            });
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
            post.setEntity(new StringEntity("", StandardCharsets.UTF_8)); // <-- empty body
            client.execute(post, response -> {
                System.out.println("POST Response: " + response.getCode() + " " + response.getReasonPhrase());
                return null;
            });
        }
    }

    public static void play() throws IOException {
        if (activeDeviceId == null) {
            findAndSetDeviceId();
            if (activeDeviceId == null) {
                System.err.println("No active device, cannot play.");
                return;
            }
        }
        sendPutRequest("https://api.spotify.com/v1/me/player/play?device_id=" + activeDeviceId);
    }

    public static void pause() throws IOException {
        if (activeDeviceId == null) {
            findAndSetDeviceId();
            if (activeDeviceId == null) {
                System.err.println("No active device, cannot pause.");
                return;
            }
        }
        sendPutRequest("https://api.spotify.com/v1/me/player/pause?device_id=" + activeDeviceId);
    }

    public static void nextTrack() throws IOException {
        if (activeDeviceId == null) {
            findAndSetDeviceId();
            if (activeDeviceId == null) {
                System.err.println("No active device, cannot skip.");
                return;
            }
        }
        sendPostRequest("https://api.spotify.com/v1/me/player/next?device_id=" + activeDeviceId);
    }

    public static void previousTrack() throws IOException {
        if (activeDeviceId == null) {
            findAndSetDeviceId();
            if (activeDeviceId == null) {
                System.err.println("No active device, cannot go back.");
                return;
            }
        }
        sendPostRequest("https://api.spotify.com/v1/me/player/previous?device_id=" + activeDeviceId);
    }

    public static String getCurrentTrack() throws IOException {
        if (accessToken == null) {
            return "Not logged in";
        }

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet("https://api.spotify.com/v1/me/player/currently-playing");
            get.setHeader("Authorization", "Bearer " + accessToken);

            return client.execute(get, response -> {
                if (response.getCode() == 204) {
                    return "Nothing playing";
                }
                if (response.getCode() != 200) {
                    System.err.println("Spotify API Error: " + response.getReasonPhrase());
                    return "Error: " + response.getReasonPhrase();
                }

                String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (json == null || json.isEmpty()) {
                    return "Nothing playing";
                }

                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

                // --- START OF NEW CODE ---
                // Check for and save the active device ID
                        if (obj.has("device") && !obj.get("device").isJsonNull()) {
                            JsonObject device = obj.getAsJsonObject("device");
                            activeDeviceId = device.get("id").getAsString();
                            // This new debug line is the one we want to see!
                            System.out.println("DEBUG (from getCurrentTrack): Active Device ID set to: " + activeDeviceId);
                }
                // --- END OF NEW CODE ---

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
    }}
