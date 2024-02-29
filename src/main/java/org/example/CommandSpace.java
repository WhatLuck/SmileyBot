package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import org.w3c.dom.Text;

import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class CommandSpace {
    private static final String CENSOR_FILE_PATH = System.getProperty("user.dir") + "/censor.txt";

    private static final String JDBC_DATABASE_URL = "jdbc:sqlite:database.db";
    public static Role serverManagerRole;
    public static Role verifiedRole;

    public static Role courseRole;
    public static Role unverifiedRole;
    public static TextChannel verifyHere;
    public static TextChannel botCommands;
    public static TextChannel recentlyVerifiedLog;

    public static String descriptionHelp = "For the bot to function you will need both your;\n" +
            "`Platform URL` and your ``API Key, found below``\n" +
            "[ - Get your UW Canvas API here -](https://canvas.uw.edu/profile/settings)\n If you are not a UW student, access your settings page on canvas.\n\n" +
            "To obtain your key, scroll down to \"Approved Integrations:\" click the blue button that says \"+ New Access Token,\" provide a reason for use, (\"Assignment Notification\" or similar) set your expiration date if you prefer, and copy the key provided. Not that your student ID can be manually parsed from your API key, so it is not necessary to find it yourself. \n\n" +
            "Then, copy down the link to your Canvas portal's URL ex. (https://canvas.uw.edu)\n" +
            "Then input your key(s) into the bot using the command\n```\n/setapi apikey: platform:\n```\n\n" +
            "For the bot to be able to access your student information, it needs access to your internal student ID, and Canvas API key, this information is stored locally and can be deleted at any time, no logs of your data will be kept outside of what is necessary.\n\n" +
            "Once your key(s) have been verified, the bot will update your username in the server to your canvas username, and provide you with the <@Verified> role.";

    public static void updateRoles(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();

        // Update ServerManager role
        List<Role> serverManagerRoles = guild.getRolesByName("ServerManager", true);
        if (!serverManagerRoles.isEmpty()) {
            serverManagerRole = serverManagerRoles.get(0);
            System.out.println("ServerManager role found: " + serverManagerRole.getName());
        } else {
            serverManagerRole = null; // Set serverManagerRole to null if the role is not found
            System.out.println("ServerManager role not found.");
        }

        // Update Verified role
        List<Role> verifiedRoles = guild.getRolesByName("Verified", true);
        if (!verifiedRoles.isEmpty()) {
            verifiedRole = verifiedRoles.get(0);
            System.out.println("Verified role found: " + verifiedRole.getName());
        } else {
            verifiedRole = null; // Set verifiedRole to null if the role is not found
            System.out.println("Verified role not found.");
        }

        // Update Unverified role
        List<Role> unverifiedRoles = guild.getRolesByName("Unverified", true);
        if (!unverifiedRoles.isEmpty()) {
            unverifiedRole = unverifiedRoles.get(0);
            System.out.println("Unverified role found: " + unverifiedRole.getName());
        } else {
            unverifiedRole = null; // Set unverifiedRole to null if the role is not found
            System.out.println("Unverified role not found.");
        }

        verifyHere = guild.getTextChannelsByName("verify-here", true).stream().findFirst().orElse(null);
        recentlyVerifiedLog = guild.getTextChannelsByName("recently-verified", true).stream().findFirst().orElse(null);
        botCommands = guild.getTextChannelsByName("bot-commands", true).stream().findFirst().orElse(null);
    }

    static void handleGetData(SlashCommandInteractionEvent event) {
        try (Connection connection = DriverManager.getConnection(JDBC_DATABASE_URL)) {
            String discordId = event.getUser().getId();
            String sql = "SELECT shortname, studentId, apiKey, discordId, platformURL FROM userData WHERE discordId = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, discordId);
                ResultSet resultSet = statement.executeQuery();
                StringBuilder responseData = new StringBuilder();
                while (resultSet.next()) {
                    String shortName = resultSet.getString("shortname");
                    String studentId = resultSet.getString("studentId");
                    String apiKey = resultSet.getString("apiKey");
                    String discordID = resultSet.getString("discordId");
                    String platformURL = resultSet.getString("platformURL"); // Fetch platform URL
                    responseData.append("**Short Name:**\n`").append(shortName).append("`\n")
                            .append("**Student ID:**\n`").append(studentId).append("`\n")
                            .append("**API Key:**\n`").append(apiKey).append("`\n")
                            .append("**Discord ID:**\n`").append(discordID).append("`\n")
                            .append("**Platform URL:**\n`").append(platformURL).append("`\n\n"); // Append platform URL
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
                .setDescription(descriptionHelp)
                .setColor(0x00b0f4)
                .setTimestamp(Instant.now());

        event.replyEmbeds(embedBuilder.build()).queue();
    }

    static void handleCommandHelp(SlashCommandInteractionEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setAuthor("Command Index", null, null)
                .setDescription("Below are the list of usable commands, \nthose listed in the field [ ADMIN ] are\nonly usable by server operators, those with \nadministrator, and the \"Server Manager\" role.")
                .addField("Help Index", "`setupHelp` `commandHelp`", false)
                .addField("API Usage", "`setApi` `getData` `deleteData`", false)
                .addField("Canvas Information", "`Todo` `GradeReport`", false)
                .addField("Server Properties [ ADMIN ]", "`resetServer` `setupServer` `toggleCensor`", false)
                .setColor(0xdef09e)
                .setTimestamp(Instant.now());

        event.replyEmbeds(embedBuilder.build()).queue();
    }

    static void handleDeleteData(SlashCommandInteractionEvent event, Guild guild, Member member) {
        updateRoles(event);
        String discordId = event.getUser().getId();
        guild.removeRoleFromMember(member, verifiedRole).queue();
        guild.addRoleToMember(member, unverifiedRole).queue();

        try (Connection connection = DriverManager.getConnection(JDBC_DATABASE_URL)) {
            // Delete user data from the database
            String deleteSql = "DELETE FROM userData WHERE discordId = ?";
            try (PreparedStatement statement = connection.prepareStatement(deleteSql)) {
                statement.setString(1, discordId);
                int rowsAffected = statement.executeUpdate();

                // Check if any rows were deleted
                if (rowsAffected > 0) {
                    event.reply("Your data has been deleted.")
                            .setEphemeral(true)
                            .queue();
                } else {
                    event.reply("You don't have any data stored.")
                            .setEphemeral(true)
                            .queue();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            event.reply("An error occurred while deleting your data.")
                    .setEphemeral(true)
                    .queue();
        }
    }

    static void handleTodo(SlashCommandInteractionEvent event) {
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

    static void handleSetAPI(SlashCommandInteractionEvent event, Member member, Guild guild) {
        String apiKey = null;
        String platformURL = null;

        updateRoles(event);

        if (!member.getRoles().contains(unverifiedRole) && !member.isOwner() && !member.hasPermission(Permission.ADMINISTRATOR) && !member.getRoles().contains(serverManagerRole)) {
            // If the member does not meet the criteria, send an ephemeral reply
            event.reply("Permission denied. Only members with the Unverified role, administrator permissions, server manager role, or server owner can run this command.")
                    .setEphemeral(true)
                    .queue();
            return;
        } else {

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

                        try {
                            event.getGuild().removeRoleFromMember(member, unverifiedRole).queue();
                            event.getGuild().addRoleToMember(member, verifiedRole).queue();
                            // Update user's nickname to the short name
                            event.getGuild().modifyNickname(member, shortName).queue();
                        } catch (HierarchyException e) {
                            // Catch the HierarchyException if cannot modify the member
                            System.out.println("Cannot modify member: " + e.getMessage());
                        } catch (NullPointerException e) {
                            // Catch the NullPointerException if verifiedRole is null (not found)
                            event.reply("The server owner has not set up the required roles yet.")
                                    .setEphemeral(true)
                                    .queue();
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

                            OffsetDateTime creationTime = member.getTimeCreated();
                            Instant instant = creationTime.toInstant();
                            ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("UTC"));

                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M-d-yyyy h:mm a");

                            EmbedBuilder embedBuilder = new EmbedBuilder()
                                    .setAuthor("Newly Verified User!")
                                    .setDescription("A new user has verified their canvas account!")
                                    .addField("Discord", member.getAsMention(), true)
                                    .addField("Account Age", formatter.format(zonedDateTime), true)
                                    .addField("Short Name", shortName, true)
                                    .setColor(0x00f549)
                                    .setFooter("User Verified")
                                    .setTimestamp(Instant.now());

                            recentlyVerifiedLog = guild.getTextChannels().stream()
                                    .filter(channel -> channel.getName().equals("recently-verified-log"))
                                    .findFirst()
                                    .orElse(null);

                            recentlyVerifiedLog.sendMessageEmbeds(embedBuilder.build()).queue();

                            event.reply("API key set successfully! Thanks " + shortName + "!")
                                    .setEphemeral(true)
                                    .queue();
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
    }

    static void setupNewServer(SlashCommandInteractionEvent event, Guild guild, Member member) {

        if (!member.isOwner() && !member.hasPermission(Permission.ADMINISTRATOR) && !member.getRoles().contains(serverManagerRole)) {
            // If the member does not meet the criteria, send an ephemeral reply
            event.reply("Permission denied. Only the server owner, members with administrator permissions, or members with the ServerManager role can run this command.")
                    .setEphemeral(true)
                    .queue();
        } else {

            serverManagerRole = guild.createRole().setName("ServerManager").setColor(Color.decode("#c95957")).complete();
            verifiedRole = guild.createRole().setName("Verified").setColor(Color.decode("#278006")).complete();
            unverifiedRole = guild.createRole().setName("Unverified").setColor(Color.decode("#6a6b6a")).complete();

            Category serverManagerCategory = guild.createCategory("ServerManager")
                    .addPermissionOverride(guild.getPublicRole(), EnumSet.noneOf(Permission.class), EnumSet.of(Permission.VIEW_CHANNEL))
                    .addPermissionOverride(serverManagerRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .complete();

            Category verifiedCategory = guild.createCategory("Verified")
                    .addPermissionOverride(guild.getPublicRole(), EnumSet.noneOf(Permission.class), EnumSet.of(Permission.VIEW_CHANNEL))
                    .addPermissionOverride(verifiedRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(serverManagerRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .complete();

            Category unverifiedCategory = guild.createCategory("Unverified")
                    .addPermissionOverride(guild.getPublicRole(), EnumSet.noneOf(Permission.class), EnumSet.of(Permission.VIEW_CHANNEL))
                    .addPermissionOverride(unverifiedRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .complete();

            serverManagerCategory.createTextChannel("Recently Verified Log").complete();
            serverManagerCategory.createTextChannel("Admin Log").complete();
            serverManagerCategory.createTextChannel("Admin chat & commands").complete();

            verifiedCategory.createTextChannel("Bot commands").complete();
            verifiedCategory.createTextChannel("verified general").complete();
            verifiedCategory.createVoiceChannel("General").complete();
            verifiedCategory.createVoiceChannel("Study 1").complete();
            verifiedCategory.createVoiceChannel("Study 2").complete();
            verifiedCategory.createVoiceChannel("Music").complete();

            unverifiedCategory.createTextChannel("Verify Here").complete();
            unverifiedCategory.createTextChannel("Unverified general").complete();

            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setAuthor("User profile setup", null, null)
                    .setDescription(descriptionHelp)
                    .setColor(0x00b0f4)
                    .setTimestamp(Instant.now());

            TextChannel verifyHere = event.getGuild().getTextChannelsByName("verify-here", true).stream().findFirst().orElse(null);
            if (verifyHere != null) {
                // Send the embed to the channel
                verifyHere.sendMessageEmbeds(embedBuilder.build()).queue();
            } else {
                System.out.println("Channel named 'verify-here' not found.");
            }


            // Add Unverified role to all members except the command executor
            guild.loadMembers().onSuccess(members -> {
                for (Member m : members) {
                    if (!m.equals(member)) {
                        guild.addRoleToMember(m, unverifiedRole).queue();
                    }
                }
            }).onError(error -> {
                System.out.println("Error loading members: " + error.getMessage());
            });


            try {
                guild.modifyRolePositions().selectPosition(serverManagerRole).moveTo(1).complete();
                guild.addRoleToMember(member, serverManagerRole).complete();
            } catch (Exception e) {
                System.out.println("An error occurred while assigning permissions to roles: " + e.getMessage());
            }
        }
    }

    static void resetServerSetup(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();

        // Check if the member is the server owner or has administrator permissions
        if (!member.isOwner() && !member.hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("Permission denied. Only the server owner or members with administrator permissions can run this command.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        // Delete roles
        for (Role role : guild.getRoles()) {
            if (role.getName().equals("ServerManager") || role.getName().equals("Verified") || role.getName().equals("Unverified")) {
                role.delete().queue();
            }
        }

        // Delete channels and categories
        for (Category category : guild.getCategories()) {
            if (category.getName().equals("ServerManager") || category.getName().equals("Verified") || category.getName().equals("Unverified")) {
                for (GuildChannel channel : category.getChannels()) {
                    channel.delete().queue();
                }
                category.delete().queue();
            }
        }

        // Reply to the user that the reset is successful
        event.reply("Reset successful. All roles, channels, and categories created by the bot have been removed.")
                .setEphemeral(true)
                .queue();
    }

    static List<PostData> getTodoAssignmentsFromCanvas(SlashCommandInteractionEvent event) throws IOException, InterruptedException {
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
                //System.out.println("Api key: " + studentAPI + "  studentID: " + studentId);
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
            //System.out.println(todoUrl + "?access_token=" + studentAPI);
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

    static void getEnrollmentInfo(SlashCommandInteractionEvent event, Member member) throws IOException, InterruptedException {

        Boolean favoriteFilter = null;
        if (event.getOption("favoritefilter") != null) {
            favoriteFilter = event.getOption("favoritefilter").getAsBoolean();
        }

        List<GradeData.EnrollmentData> enrollments = GradeData.fetchCourseData(member.getId(), favoriteFilter);
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setAuthor("User Grade Report")
                .setColor(Color.decode("#7d47b3"))
                .setTimestamp(Instant.now());

        StringBuilder descriptionBuilder = new StringBuilder();
        descriptionBuilder.append("Below are your current grades in your enrollments:\n\n");

        for (GradeData.EnrollmentData enrollment : enrollments) {
            descriptionBuilder.append("**Course Name:** ").append(enrollment.getCourseName()).append("\n");
            descriptionBuilder.append("> Current Grade: **").append(enrollment.getCourseGrade()).append("**\n\n");
        }

        embedBuilder.setDescription(descriptionBuilder.toString());

        event.replyEmbeds(embedBuilder.build()).queue();

    }

    static void applyCourseRoles(SlashCommandInteractionEvent event, Member member, Guild guild) {

        Boolean favoriteFilter = null;
        if (event.getOption("favoritefilter") != null) {
            favoriteFilter = event.getOption("favoritefilter").getAsBoolean();
        }

        List<GradeData.CourseData> courses = GradeData.fetchCourseName(member.getId(), favoriteFilter);

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setAuthor("Roles Applied!")
                .setDescription("Your roles have been assigned based on your currently enrolled courses!\n\nRoles added:")
                .setColor(0x00b0f4); // Blue color

        for (GradeData.CourseData course : courses) {
            String roleName = "" + course.getCourseId();

            List<Role> existingRoles = guild.getRolesByName(roleName, true);
            Role courseRole;

            if (existingRoles.isEmpty()) {
                courseRole = guild.createRole()
                        .setName(roleName)
                        .setColor(Color.decode("#b3b3b3"))
                        .complete();
            } else {
                courseRole = existingRoles.get(0); // Assuming there's only one role with the same name
            }

            guild.addRoleToMember(member, courseRole).complete();

            // Append the role name to the embed description
            embedBuilder.appendDescription("`" + roleName + "` ");
        }

        // Build the message embed
        MessageEmbed embed = embedBuilder.build();

        // Reply with the embed message (ephemeral)
        event.replyEmbeds(embed).queue();
    }

    static List<PostData> getGradingReport(SlashCommandInteractionEvent event) throws IOException, InterruptedException {
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
                //System.out.println("Api key: " + studentAPI + "  studentID: " + studentId);
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
            //System.out.println(todoUrl + "?access_token=" + studentAPI);
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

    static boolean apiFailure(SlashCommandInteractionEvent event, Member member) {
        String studentAPI = null;
        String platformURL = null;

        try (Connection connection = DriverManager.getConnection(JDBC_DATABASE_URL)) {
            String sql = "SELECT apiKey, platformURL FROM userData WHERE discordId = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, member.getId());
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    studentAPI = resultSet.getString("apiKey");
                    platformURL = resultSet.getString("platformURL");
                    //System.out.println("Api key: " + studentAPI);
                }
            }
        } catch (SQLException e) {
            // SQL Exception occurred, reply with embed and return false
            return false;
        }

        // If platformURL is null or doesn't start with "http", return false
        if (platformURL == null || !platformURL.startsWith("http")) {
            System.err.println("Invalid platformURL: " + platformURL);
            return false;
        }

        try {
            String courseURL = platformURL + "/api/v1/users/self/courses?include[]=total_scores&include[]=term&include[]=favorites&access_token=" + studentAPI;
            //System.out.println(courseURL);
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(courseURL))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return true;
            } else {
                // If HTTP request fails, reply with embed and return false
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setAuthor("API Failure!")
                        .setDescription("Failed to retrieve course information from the platform. Check your data with /getData!")
                        .setColor(0xd74242) // Red color
                        .setTimestamp(Instant.now());
                MessageEmbed embed = embedBuilder.build();
                event.replyEmbeds(embed).setEphemeral(true).queue();
                return false;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    static void sendAPIFailure(SlashCommandInteractionEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setAuthor("API Failure!")
                .setDescription("An error occurred while contacting the platform.")
                .setColor(0xd74242) // Red color
                .setTimestamp(Instant.now());
        MessageEmbed embed = embedBuilder.build();
        event.replyEmbeds(embed).setEphemeral(true).queue();
    }

    public static void removeCourseRoles(SlashCommandInteractionEvent event, Member member) {
        List<Role> roles = member.getRoles();
        for (Role role : roles) {
            // Check if the role is denoted by its color and name
            if (isCourseRole(role)) {
                role.getGuild().removeRoleFromMember(member, role).queue();
            }
        }
        sendSuccessEmbed(event);
    }

    private static void sendSuccessEmbed(SlashCommandInteractionEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setAuthor("Success!")
                .setDescription("All your course roles have been removed!\nTo get them back run /applyCourseRoles!")
                .setColor(new Color(0x9cb9e8))
                .setTimestamp(Instant.now());

        MessageEmbed embed = embedBuilder.build();
        event.replyEmbeds(embed).queue();
    }

    private static boolean isCourseRole(Role role) {
        // Check if the role name consists only of digits
        boolean isDigitsOnly = role.getName().matches("\\d+");
        // Check if the role color is a specific color (e.g., Color.decode("#b3b3b3"))
        boolean isCourseColor = role.getColor().equals(Color.decode("#b3b3b3"));

        return isDigitsOnly && isCourseColor;
    }

    static String sendGetRequest(String urlString) throws IOException {
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


    // Map to store the current embed and page index for each guild
    static Map<Long, Map.Entry<MessageEmbed, Integer>> embedMap = new HashMap<>();

    public static void sendCreationOptions(SlashCommandInteractionEvent event, Member member, int pageIndex) {
        List<GradeData.CourseData> courses = GradeData.fetchCourseName(member.getId(), false);
        int coursesPerPage = 1; // Display only one course per page
        int pageCount = (int) Math.ceil((double) courses.size() / coursesPerPage);

        // Calculate the start and end indices based on the pageIndex
        int startIndex = pageIndex * coursesPerPage;
        int endIndex = Math.min(startIndex + coursesPerPage, courses.size());

        // Initialize the embed builder
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setAuthor("Creation Option(s)")
                .setDescription("Below are the courses that you are enrolled in, please choose one to create a channel for!\n\n")
                .setColor(new Color(0x00b0f4))
                .setTimestamp(Instant.now());

        // Loop through the courses and add buttons
        for (int j = startIndex; j < endIndex; j++) {
            GradeData.CourseData course = courses.get(j);
            String description = "> " + course.getCourseName() + "\n" + "```Course Code: " + course.getCourseId() + "```\n\n";
            embedBuilder.appendDescription(description);

            // Add courseId to the button ID
            String createButtonId = "create " + course.getCourseId() + " " + course.getCourseName();

            // Check if there are more pages to add navigation buttons
            boolean hasPrevious = pageIndex > 0;
            boolean hasNext = (pageIndex + 1) < pageCount;

            // Store the embed and page index in the embedMap
            embedMap.put(event.getGuild().getIdLong(), new AbstractMap.SimpleEntry<>(embedBuilder.build(), pageIndex));

            // Reply with the embed and all action rows
            event.replyEmbeds(embedBuilder.build())
                    .addActionRow(
                            Button.secondary("previous " + (pageIndex - 1), "◀️ Previous").withDisabled(!hasPrevious),
                            Button.success(createButtonId, "Create!"),
                            Button.secondary("next " + (pageIndex + 1), "Next ▶️").withDisabled(!hasNext)
                    )
                    .queue();
        }
    }


    public static void updateEmbed(ButtonInteractionEvent event, int pageIndex) {
        long guildId = event.getGuild().getIdLong();

        // Retrieve the embed and page index from the embedMap
        Map.Entry<MessageEmbed, Integer> embedData = embedMap.get(guildId);
        MessageEmbed previousEmbed = embedData.getKey();
        int previousPageIndex = embedData.getValue();

        // Delete the previous message
        event.getChannel().deleteMessageById(event.getMessageId()).queue(
                // On success, send the updated embed
                (__) -> {
                    // Retrieve the courses
                    List<GradeData.CourseData> courses = GradeData.fetchCourseName(event.getMember().getId(), false);
                    int coursesPerPage = 1; // Display only one course per page
                    int pageCount = (int) Math.ceil((double) courses.size() / coursesPerPage);

                    // Calculate the new page index
                    int newIndex = (pageIndex + 1) % pageCount;

                    // Calculate the start and end indices based on the new page index
                    int startIndex = newIndex * coursesPerPage;
                    int endIndex = Math.min(startIndex + coursesPerPage, courses.size());

                    // Rebuild the embed with the updated page index
                    EmbedBuilder embedBuilder = new EmbedBuilder()
                            .setAuthor("Creation Option(s)")
                            .setDescription("Below are the courses that you are enrolled in, please choose one to create a channel for!\n\n")
                            .setColor(new Color(0x00b0f4))
                            .setTimestamp(Instant.now());

                    // Loop through the courses and add buttons
                    for (int j = startIndex; j < endIndex; j++) {
                        GradeData.CourseData course = courses.get(j);
                        String description = "> " + course.getCourseName() + "\n" + "```Course Code: " + course.getCourseId() + "```\n\n";
                        embedBuilder.appendDescription(description);

                        // Add courseId to the button ID
                        String createButtonId = "create " + course.getCourseId() + " " + course.getCourseName();


                        // Check if there are more pages to add navigation buttons
                        boolean hasPrevious = newIndex > 0;
                        boolean hasNext = (newIndex + 1) < pageCount;

                        // Update the embedMap with the new embed and page index
                        embedMap.put(guildId, new AbstractMap.SimpleEntry<>(embedBuilder.build(), newIndex));

                        // Send the updated embed with navigation buttons
                        event.getChannel().sendMessageEmbeds(embedBuilder.build())
                                .addActionRow(
                                        Button.secondary("previous " + (newIndex - 1), "◀️ Previous").withDisabled(!hasPrevious),
                                        Button.success(createButtonId, "Create!"),
                                        Button.secondary("next " + (newIndex + 1), "Next ▶️").withDisabled(!hasNext)
                                )
                                .queue();
                    }
                },
                // On failure, log an error
                (error) -> System.out.println("Embed modification failure!")
        );
    }


    // Helper method to format course list
    private static String getCourseList(List<GradeData.CourseData> courses, Member member) {

        StringBuilder sb = new StringBuilder();

        for (GradeData.CourseData course : courses) {
            sb.append("> ").append(course.getCourseName()).append("\n")
                    .append("```Course Code: ").append(course.getCourseId()).append("```").append("\n\n");
        }
        return sb.toString();
    }

    public static void createCourseChannel(ButtonInteractionEvent event, Guild guild, String courseID, String courseName) {
        // Check if the role for the courseID exists
        Role courseRole = guild.getRolesByName(courseID, true).stream().findFirst().orElse(null);
        if (courseRole == null) {
            event.reply("The role for course ID " + courseID + " does not exist.").queue();
            return;
        }

        // Check if the user running the command has the course role, is a server admin, or has the serverManagerRole
        Member member = event.getMember();
        if (!member.getRoles().contains(courseRole) && !member.isOwner() && !member.hasPermission(Permission.ADMINISTRATOR) && !member.getRoles().contains(serverManagerRole)) {
            event.reply("You do not have permission to create a channel for this course. Check your roles!").queue();
            return;
        }

        // Check if the channel has already been created
        Category existingChannel = guild.getCategoriesByName(courseName, true).stream().findFirst().orElse(null);
        if (existingChannel != null) {
            event.reply("The channel for course " + courseName + " has already been created.").queue();
            return;
        }

        // Proceed with creating the course channel
        Category courseChannel = guild.createCategory(courseName)
                .addPermissionOverride(guild.getPublicRole(), EnumSet.noneOf(Permission.class), EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(courseRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(event.getGuild().getRolesByName("ServerManager", true).stream().findFirst().orElse(null), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .complete();

        TextChannel courseChannelGeneral = (TextChannel) courseChannel.createTextChannel("General").complete();
        courseChannel.createTextChannel("Homework Discussion").complete();
        courseChannel.createTextChannel("Important info").complete();
        courseChannel.createVoiceChannel("Voice Channel").complete();

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setAuthor("Course Channel Created!")
                .setDescription("The channel for\n`" + courseName + "`\nCan be found here;  <#" + courseChannelGeneral.getId() + ">")
                .setColor(Color.decode("#00f510"))
                .setTimestamp(Instant.now());

        event.replyEmbeds(embedBuilder.build()).queue();
    }


    public static void promptDeleteAll(SlashCommandInteractionEvent event, Guild guild, Member member){
        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor("Are you sure?")
                .setDescription("This will **permanently** delete all messages, \nembeds, and channels within this category!\n\nPlease confirm the deletion of this category below.")
                .setColor(Color.decode("#940000"))
                .setTimestamp(Instant.now());

        event.replyEmbeds(embed.build()).addActionRow(
                Button.success("confirmDeletion", "Confirm"),
                Button.danger("cancelDeletion", "Cancel")
        ).queue();
    }

    public static void deleteAllInCategory(ButtonInteractionEvent event, Guild guild, Member member) {
        // Check if the member is a server manager or administrator
        if (!member.hasPermission(Permission.ADMINISTRATOR) && !member.getRoles().contains(serverManagerRole)) {
            event.reply("You do not have permission to perform this action.").queue();
            return;
        }

        // Get the category of the channels you want to delete (replace "Category Name" with the actual category name)
        Category category = event.getChannel().asTextChannel().getParentCategory();

        if (category != null) {
            category.getChannels().forEach(channel -> channel.delete().queue());
            category.delete().queue();
            event.reply("All channels in the category have been deleted.").queue();
        } else {
            event.reply("Failed to delete channels: Category not found.").queue();
        }
    }

}
