package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CommandSpace {
    private static final String JDBC_DATABASE_URL = "jdbc:sqlite:database.db";
    static void handleGetData(SlashCommandInteractionEvent event) {
        try (Connection connection = DriverManager.getConnection(JDBC_DATABASE_URL)) {
            String discordId = event.getUser().getId();
            String sql = "SELECT shortname, studentId, apiKey, discordId FROM userData WHERE discordId = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, discordId);
                ResultSet resultSet = statement.executeQuery();
                StringBuilder responseData = new StringBuilder();
                while (resultSet.next()) {
                    String shortName = resultSet.getString("shortname");
                    String studentId = resultSet.getString("studentId");
                    String apiKey = resultSet.getString("apiKey");
                    String discordID = resultSet.getString("discordId");
                    responseData.append("**Short Name:**\n`").append(shortName).append("`\n")
                            .append("**Student ID:**\n`").append(studentId).append("`\n")
                            .append("**API Key:**\n`").append(apiKey).append("`\n")
                            .append("**Discord ID:**\n`").append(discordID).append("`\n\n");
                }
                if (responseData.length() > 0) {
                    EmbedBuilder embedBuilder = new EmbedBuilder()
                            .setAuthor("User information", "https://canvas.uw.edu/profile")
                            .setDescription(responseData.toString())
                            .setColor(0x5c5c5c);
                    event.getUser().openPrivateChannel().queue(privateChannel -> {
                        privateChannel.sendMessageEmbeds(embedBuilder.build()).queue();
                    });
                    event.reply("Your user data has been sent to your DMs.").setEphemeral(true).queue();
                } else {
                    event.reply("No user data found.").setEphemeral(true).queue();
                }
                resultSet.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            event.reply("An error occurred while fetching your data.").setEphemeral(true).queue();
        }
    }
    static void handleSetupHelp(SlashCommandInteractionEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setAuthor("User profile setup", null, null)
                .setDescription("For the bot to function you will need both your;\n" +
                        "`Platform URL` and your ``API Key, found below``\n" +
                        "[ - Get your UW Canvas API here -](https://canvas.uw.edu/profile/settings)\n If you are not a UW student, access your settings page on canvas.\n\n" +
                        "To obtain your key, scroll down to \"Approved Integrations:\" click the blue button that says \"+ New Access Token,\" provide a reason for use, (\"Assignment Notification\" or similar) set your expiration date if you prefer, and copy the key provided. Not that your student ID can be manually parsed from your API key, so it is not necessary to find it yourself. \n\n" +
                        "Then, copy down the link to your Canvas portal's URL ex. (https://canvas.uw.edu)\n" +
                        "Then input your key(s) into the bot using the command\n```\n/setapi apikey: platform:\n```\n\n" +
                        "For the bot to be able to access your student information, it needs access to your internal student ID, and Canvas API key, this information is stored locally and can be deleted at any time, no logs of your data will be kept outside of what is necessary.\n\n" +
                        "Once your key(s) have been verified, the bot will update your username in the server to your canvas username, and provide you with the <@123> role.")
                .setColor(0x00b0f4)
                .setTimestamp(Instant.now());

        event.replyEmbeds(embedBuilder.build()).queue();
    }
    static void handleDeleteData(SlashCommandInteractionEvent event) {
        String discordId = event.getUser().getId();
        Guild guild = event.getGuild();
        Member member = guild.getMemberById(discordId);
        Role verifiedRole = guild.getRolesByName("Verified", true).stream().findFirst().orElse(null);

        if (member != null && verifiedRole != null) {
            guild.removeRoleFromMember(member, verifiedRole).queue();
        }

        try (Connection connection = DriverManager.getConnection(JDBC_DATABASE_URL)) {
            // Delete user data from the database
            String deleteSql = "DELETE FROM userData WHERE discordId = ?";
            try (PreparedStatement statement = connection.prepareStatement(deleteSql)) {
                statement.setString(1, discordId);
                int rowsAffected = statement.executeUpdate();

                // Check if any rows were deleted
                if (rowsAffected > 0) {
                    event.reply("Your data has been deleted.").queue();
                } else {
                    event.reply("You don't have any data stored.").queue();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            event.reply("An error occurred while deleting your data.").queue();
        }
    }
    static void handleTodo(SlashCommandInteractionEvent event){
        try {
            List<PostData> todoAssignments = getTodoAssignmentsFromCanvas(event);

            if (todoAssignments.isEmpty()) {
                event.reply("No TODO assignments found.").queue();
                return;
            }

            // Send each assignment as a separate message
            // Check if there are assignments to send
            if (!todoAssignments.isEmpty()) {
                boolean firstAssignment = true;
                for (PostData assignment : todoAssignments) {
                    EmbedBuilder embedBuilder = new EmbedBuilder()
                            .setTitle(assignment.getName(), assignment.getHtmlUrl())
                            .setThumbnail("https://www.instructure.com/sites/default/files/image/2021-12/Canvas_logo_single_mark.png")
                            .setDescription(assignment.getDescription())
                            .addField("Due Date", assignment.getDueAt(), true)
                            .addField("Points Possible", assignment.getPointsPossible() + " Points", true)
                            .addField("Course", assignment.getCourseName(), false)
                            .setColor(0x00b0f4)
                            .setTimestamp(Instant.now());

                    if (firstAssignment) {
                        event.replyEmbeds(embedBuilder.build()).queue();
                        firstAssignment = false;
                    } else {
                        event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
                    }
                }
            } else {
                event.reply("No TODO assignments found.").queue();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            event.reply("Error occurred while fetching TODO assignments.").setEphemeral(true).queue();
        }
    }
    static void handleSetAPI(SlashCommandInteractionEvent event){
        String apiKey = null;
        String platformURL = null;

        // Check if options exist and parse them
        if (event.getOption("apikey") != null) {
            apiKey = event.getOption("apikey").getAsString();
        }
        if (event.getOption("platform") != null) {
            platformURL = event.getOption("platform").getAsString();
        }

        String discordId = event.getUser().getId();

        if (apiKey != null) {
            try {
                HttpClient httpClient = HttpClient.newHttpClient();
                String url = platformURL + "/api/v1/users/self?access_token=" + apiKey;
                HttpRequest request = HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(url))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode rootNode = objectMapper.readTree(response.body());
                    String studentId = rootNode.path("id").asText();
                    String shortName = rootNode.path("short_name").asText();

                    // Check if the "Verified" role exists, create it if not
                    Role verifiedRole = event.getGuild().getRolesByName("Verified", true).isEmpty()
                            ? event.getGuild().createRole().setName("Verified").complete()
                            : event.getGuild().getRolesByName("Verified", true).get(0);

                    Member member = event.getGuild().getMemberById(discordId);


                    try {
                        event.getGuild().addRoleToMember(member, verifiedRole).queue();

                        // Update user's nickname to the short name
                        event.getGuild().modifyNickname(member, shortName).queue();
                    } catch (HierarchyException e) {
                        // Catch the HierarchyException if cannot modify the member
                        System.out.println("Cannot modify member: " + e.getMessage());
                    }

                    try (Connection connection = DriverManager.getConnection(JDBC_DATABASE_URL)) {
                        createTableIfNotExists(connection);

                        String sql = "INSERT INTO userData (studentid, apiKey, discordId, shortname, platformURL) VALUES (?, ?, ?, ?, ?)";
                        try (PreparedStatement statement = connection.prepareStatement(sql)) {
                            statement.setString(1, studentId);
                            statement.setString(2, apiKey);
                            statement.setString(3, discordId);
                            statement.setString(4, shortName);
                            statement.setString(5, platformURL);  // Add platformURL parameter
                            statement.executeUpdate();
                        }

                        event.reply("API key set successfully! Thanks " + shortName + "!").queue();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        event.reply("Error occurred while setting the API key.").setEphemeral(true).queue();
                    }
                } else {
                    event.reply("Invalid API key.").setEphemeral(true).queue();
                }
            } catch (Exception e) {
                e.printStackTrace();
                event.reply("Error occurred while verifying the API key.").setEphemeral(true).queue();
            }
        } else {
            event.reply("Please provide the API key.").setEphemeral(true).queue();
        }
    }


    private static List<PostData> getTodoAssignmentsFromCanvas(SlashCommandInteractionEvent event) throws IOException, InterruptedException {
        String studentId = null;
        String studentAPI = null;
        String platformURL = null;

        // Retrieve the student API key, student ID, and platform URL from the database
        String sql = "SELECT apiKey, studentId, platformURL FROM userData WHERE discordId = ?";
        try (Connection connection = DriverManager.getConnection(JDBC_DATABASE_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, event.getUser().getId());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                studentAPI = resultSet.getString("apiKey");
                studentId = resultSet.getString("studentId");
                platformURL = resultSet.getString("platformURL");
                System.out.println("Api key: " + studentAPI + "  studentID: " + studentId);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // If both student API key, student ID, and platform URL are retrieved, proceed with the API request
        if (studentAPI != null && studentId != null && platformURL != null) {
            String todoUrl = platformURL + "/api/v1/users/self/todo"; // Construct the API request URL using platformURL
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(todoUrl + "?access_token=" + studentAPI))
                    .build();
            System.out.println(todoUrl + "?access_token=" + studentAPI);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return PostData.parseJSONAssignments(response.body());
            } else {
                System.out.println("Failed to retrieve TODO assignments. HTTP Status: " + response.statusCode());
            }
        } else {
            System.out.println("Failed to retrieve API key, student ID, or platform URL from the database.");
        }

        return new ArrayList<>(); // Return an empty list if the API request fails or necessary data is missing
    }

    static void createTableIfNotExists(Connection connection) throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS userData (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "studentid TEXT," +
                "apiKey TEXT," +
                "discordid TEXT," +
                "shortname TEXT," +
                "platformURL TEXT" +  // Add platformURL column
                ")";
        try (var statement = connection.createStatement()) {
            statement.execute(createTableSQL);
        }
    }

    private String sendGetRequest(String urlString) throws IOException {
        StringBuilder response = new StringBuilder();

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }

        return response.toString();
    }
}
