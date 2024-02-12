package org.example;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.*;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MyListener extends ListenerAdapter {
    @Override
    public void onReady(ReadyEvent event) {
        super.onReady(event);
        for (Guild guild : event.getJDA().getGuilds()) {
            registerCommands(guild);
        }
    }
    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        super.onGuildJoin(event);
        registerCommands(event.getGuild());
    }

    private void registerCommands(Guild guild) {
        CommandListUpdateAction commands = guild.updateCommands();
        commands.addCommands(
                Commands.slash("setapi", "Set user API key")
                        .addOption(OptionType.STRING, "studentid", "The student ID")
                        .addOption(OptionType.STRING, "apikey", "The API key"),
                Commands.slash("getdata", "Get user data"),
                Commands.slash("todo", "Finds the user's todo assignments")
        ).queue();
    }
    private final String JDBC_DATABASE_URL = "jdbc:sqlite:C:/Users/slend/IdeaProjects/mega-balls-29/database.db";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        try (Connection connection = DriverManager.getConnection(JDBC_DATABASE_URL)) {
            createTableIfNotExists(connection);
        } catch (SQLException e) {
            e.printStackTrace();
            event.reply("Error occurred while accessing the database.").setEphemeral(true).queue();
            return;
        }
        if (event.getName().equals("setapi")) {
            String studentId = null;
            String apiKey = null;

            // Check if options exist and parse them
            if (event.getOption("studentid") != null) {
                studentId = event.getOption("studentid").getAsString();
            }
            if (event.getOption("apikey") != null) {
                apiKey = event.getOption("apikey").getAsString();
            }

            String discordId = event.getUser().getId();

            if (studentId != null && apiKey != null) {
                // Connect to SQLite database and insert user data into the database
                try (Connection connection = DriverManager.getConnection(JDBC_DATABASE_URL)) {
                    // Create the 'userData' table if it doesn't exist
                    createTableIfNotExists(connection);

                    // Insert user data into the database
                    String sql = "INSERT INTO userData (studentId, apiKey, discordId) VALUES (?, ?, ?)";
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, studentId);
                        statement.setString(2, apiKey);
                        statement.setString(3, discordId);
                        statement.executeUpdate();
                    }

                    event.reply("API key set successfully!").queue();
                } catch (SQLException e) {
                    e.printStackTrace();
                    event.reply("Error occurred while setting the API key.").setEphemeral(true).queue();
                }
            } else {
                event.reply("Please provide both student ID and API key.").setEphemeral(true).queue();
            }
        } else if (event.getName().equals("getdata")) {
            // Connect to SQLite database and fetch user data attributed to the user's Discord ID
            try (Connection connection = DriverManager.getConnection(JDBC_DATABASE_URL)) {
                String discordId = event.getUser().getId();
                String sql = "SELECT studentId, apiKey FROM userData WHERE discordId = ?";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, discordId);
                    ResultSet resultSet = statement.executeQuery();

                    // Process the retrieved data
                    StringBuilder responseData = new StringBuilder();
                    while (resultSet.next()) {
                        String studentId = resultSet.getString("studentId");
                        String apiKey = resultSet.getString("apiKey");
                        responseData.append("Student ID: ").append(studentId).append("\n")
                                .append("API Key: ").append(apiKey).append("\n\n");
                    }

                    // Send the fetched data as a direct message to the user
                    if (responseData.length() > 0) {
                        event.getUser().openPrivateChannel().queue(privateChannel -> {
                            privateChannel.sendMessage("Your user data:\n" + responseData.toString()).queue();
                        });
                    } else {
                        event.reply("No user data found.").queue();
                    }

                    resultSet.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                event.reply("Error occurred while fetching user data.").queue();
            }
        } else if (event.getName().equals("todo")) {
            try {
                List<PostData> todoAssignments = getTodoAssignmentsFromCanvas(event);

                if (todoAssignments.isEmpty()) {
                    event.reply("No TODO assignments found.").queue();
                    return;
                }

                // Send each assignment as a separate message
                // Check if there are assignments to send
                if (!todoAssignments.isEmpty()) {
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

                        event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
                    }
                } else {
                    event.reply("No TODO assignments found.").queue();
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                event.reply("Error occurred while fetching TODO assignments.").setEphemeral(true).queue();
            }
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

    private List<PostData> getTodoAssignmentsFromCanvas(SlashCommandInteractionEvent event) throws IOException, InterruptedException {
        String studentId = null;
        String studentAPI = null;
        String todoUrl = "https://canvas.uw.edu/api/v1/users/self/todo"; // Placeholder for student ID

        // Retrieve the student API key and student ID from the database
        String sql = "SELECT apiKey, studentId FROM userData WHERE discordId = ?";
        try (Connection connection = DriverManager.getConnection(JDBC_DATABASE_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, event.getUser().getId());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                studentAPI = resultSet.getString("apiKey");
                studentId = resultSet.getString("studentId");
                System.out.println("Api key: " + studentAPI + "  studentID: " + studentId);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // If both student API key and student ID are retrieved, proceed with the API request
        if (studentAPI != null && studentId != null) {
            String formattedUrl = String.format(todoUrl, studentId); // Substitute the student ID in the URL
            System.out.println(formattedUrl);
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(formattedUrl + "?access_token=" + studentAPI))
                    .build();
            System.out.println(formattedUrl + "?access_token=" + studentAPI);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return PostData.parseJSONAssignments(response.body());
            } else {
                System.out.println("Failed to retrieve TODO assignments. HTTP Status: " + response.statusCode());
            }
        } else {
            System.out.println("Failed to retrieve API key or student ID from the database.");
        }

        return new ArrayList<>(); // Return an empty list if the API request fails or necessary data is missing
    }


    private void createTableIfNotExists(Connection connection) throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS userData (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "studentid TEXT," +
                "apikey TEXT," +
                "discordid TEXT" +
                ")";
        try (var statement = connection.createStatement()) {
            statement.execute(createTableSQL);
        }
    }

}
