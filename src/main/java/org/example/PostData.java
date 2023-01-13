package org.example;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class PostData {
    private static int postId;
    private static String url;

    public void setUrl(String url) {
        PostData.url = url;
    }
    public static String getUrl() {
        return url;
    }
    public void setPostId(int postId) {
        PostData.postId = postId;
    }
    public static int getPostId() {
        return postId;
    }

    public PostData(String url, int postId) {
        PostData.url = url;
        PostData.postId = postId;
    }

    public static PostData getPostsFromE621(String tags) throws IOException, InterruptedException {
        String url = "https://e621.net/posts.json?tags=" + tags + "+order:random&limit=1";

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response.body());
            JsonNode postNode = jsonNode.get("posts");
            if (postNode.isArray() && postNode.size() > 0) {
                JsonNode fileNode = postNode.get(0).get("file");
                String imageUrl = fileNode.get("url").asText();
                int postId = postNode.get(0).get("id").asInt();
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(imageUrl).openConnection();
                    connection.setRequestMethod("HEAD");
                    if (connection.getResponseCode() == 200) {
                        return new PostData(imageUrl, postId);
                    } else {
                        // Handle error
                    }
                } catch (IOException e) {
                    // Handle error
                }
            } else {
                // Handle error
            }
        } else {
            // Handle error
        }
        return null;
    }


}
