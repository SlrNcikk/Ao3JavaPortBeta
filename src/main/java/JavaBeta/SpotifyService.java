package JavaBeta;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class SpotifyService {
    private static final String CLIENT_ID = "60427f7ee95a4f2894d82fc5658e11a0";
    private static final String REDIRECT_URI = "http://127.0.0.1:8888/callback";
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";

    private static String accessToken;

    public static void authenticate() throws IOException {
        String authUri = AUTH_URL + "?client_id=" + CLIENT_ID +
                "&response_type=code&redirect_uri=" + REDIRECT_URI +
                "&scope=user-modify-playback-state user-read-playback-state";
        Desktop.getDesktop().browse(URI.create(authUri));
    }

    public static void setAccessToken(String token) {
        accessToken = token;
    }

    private static void sendPutRequest(String url) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPut put = new HttpPut(url);
            put.setHeader("Authorization", "Bearer " + accessToken);
            client.execute(put);
        }
    }

    public static void play() throws IOException {
        sendPutRequest("https://api.spotify.com/v1/me/player/play");
    }

    public static void pause() throws IOException {
        sendPutRequest("https://api.spotify.com/v1/me/player/pause");
    }

    public static void nextTrack() throws IOException {
        sendPostRequest("https://api.spotify.com/v1/me/player/next");
    }

    public static void previousTrack() throws IOException {
        sendPostRequest("https://api.spotify.com/v1/me/player/previous");
    }

    private static void sendPostRequest(String url) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", "Bearer " + accessToken);
            client.execute(post);
        }
    }

    public static String getCurrentTrack() throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet("https://api.spotify.com/v1/me/player/currently-playing");
            get.setHeader("Authorization", "Bearer " + accessToken);
            var response = client.execute(get);
            var json = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("item")) {
                JsonObject item = obj.getAsJsonObject("item");
                String name = item.get("name").getAsString();
                String artist = item.getAsJsonArray("artists").get(0)
                        .getAsJsonObject().get("name").getAsString();
                return name + " — " + artist;
            }
        }
        return "Nothing playing";
    }
}
