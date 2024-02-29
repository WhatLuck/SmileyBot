package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.hooks.*;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.jetbrains.annotations.NotNull;

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
import java.util.*;
import java.awt.*;
import java.util.List;

import static org.example.CommandSpace.*;

public class MyListener extends ListenerAdapter {


    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        // Extract action from the component ID and trim any leading/trailing spaces
        String action = componentId.split(" ")[0].trim();

        // Switch based on the action
        switch (action) {
            case "previous":
                // Handle the previous button click
                int prevIndex = Integer.parseInt(componentId.substring("previous".length()).replaceAll("\\s+", "")) - 1;
                deleteAndThenUpdate(event, prevIndex);
                break;
            case "next":
                // Handle the next button click
                // Extract the numeric part of the componentId (without spaces) and parse it to an integer
                int nextIndex = Integer.parseInt(componentId.substring("next".length()).replaceAll("\\s+", "")) + 1;

                deleteAndThenUpdate(event, nextIndex);
                break;
            case "create":
                // Extract the courseID and courseName from the componentId
                String[] parts = componentId.split("\\s+", 3); // Split into three parts based on whitespace
                if (parts.length == 3) {
                    String courseID = parts[1]; // Extract courseID
                    String courseName = parts[2]; // Extract courseName
                    createCourseChannel(event, event.getGuild(), courseID, courseName);
                } else {
                    System.out.println("Invalid componentId format: " + componentId);
                }
                break;
            case "cancelDeletion":
                event.getMessage().delete().queue(
                        // On success, do nothing
                        __ -> {},
                        // On failure, log an error
                        error -> System.out.print("")
                );
                break;
            case "confirmDeletion":
                CommandSpace.deleteAllInCategory(event, event.getGuild(), event.getMember());
                break;
            default:
                System.out.println("Unknown action: " + action);
                break;
        }
    }

    private void deleteAndThenUpdate(ButtonInteractionEvent event, int pageIndex) {
        // Run updateEmbed first
        updateEmbed(event, pageIndex);

        // Delete the current embed message
        event.getMessage().delete().queue(
                // On success, do nothing
                (__)->{},
                // On failure, log an error
                (error) -> System.out.print("")
        );
    }


    public static void registerCommands(Guild guild) {
        CommandListUpdateAction commands = guild.updateCommands();
        commands.addCommands(
                Commands.slash("setapi", "Set user API key")
                        .addOption(OptionType.STRING, "apikey", "The API key")
                        .addOption(OptionType.STRING, "platform", "Your canvas based website url"),
                Commands.slash("getdata", "Get user data"),
                Commands.slash("todo", "Finds the user's todo assignments"),
                Commands.slash("setuphelp", "Help page for user profiling"),
                Commands.slash("deletedata", "Removes user data from database"),
                Commands.slash("setupserver", "Not accessible to regular users"),
                Commands.slash("resetserver", "Not accessible to regular users"),
                Commands.slash("gradereport", "Accesses user's enrolled course and pulls grades")
                        .addOption(OptionType.BOOLEAN, "favoritefilter", "Filters the report by only favorited courses"),
                Commands.slash("commandhelp", "Lists all usable commands"),
                //Commands.slash("togglecensor", "Toggles the bot's censor [ PER INDIVIDUAL CHANNEL ]"),
                Commands.slash("createstudychannel", "Creates a temporary study channel, inviting users through an embed"),
                Commands.slash("applycourseroles", "Parses user's currently enrolled courses and applies roles for each")
                        .addOption(OptionType.BOOLEAN, "favoritefilter", "Filters the report by only favorited courses"),
                Commands.slash("clearcourseroles", "Removes all course-based roles"),
                Commands.slash("createcoursecategory", "Creates a category for a specific course [ REQUIRES COURSE ROLE ]"),
                Commands.slash("deletecoursecategory", "Deletes all channels within the course channel [ ADMIN ONLY ]")
                // Ticket Handler?
                ).queue();
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        super.onReady(event);
        for (Guild guild : event.getJDA().getGuilds()) {
            registerCommands(guild);
        }
    }
    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        super.onGuildJoin(event);
        registerCommands(event.getGuild());
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        // Retrieve the @Unverified role
        Role unverifiedRole = event.getGuild().getRolesByName("Unverified", true).stream().findFirst().orElse(null);
        if (unverifiedRole != null) {
            // Add the @Unverified role to the new member
            event.getGuild().addRoleToMember(event.getMember(), unverifiedRole).queue();
        } else {
            System.out.println("Error: @Unverified role not found!");
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        String JDBC_DATABASE_URL = "jdbc:sqlite:database.db";
        try (Connection connection = DriverManager.getConnection(JDBC_DATABASE_URL)) {
            CommandSpace.createTableIfNotExists(connection);
        } catch (SQLException e) {
            e.printStackTrace();
            event.reply("Error occurred while accessing the database.").setEphemeral(true).queue();
            return;
        }
        Member member = event.getMember();

        switch (event.getName()) {
            case "setapi" -> {
                assert member != null;
                CommandSpace.handleSetAPI(event, member, event.getGuild());
            }
            case "getdata" -> CommandSpace.handleGetData(event);
            case "todo" -> {
                if (CommandSpace.apiFailure(event, member)) CommandSpace.handleTodo(event);
                else {
                    CommandSpace.sendAPIFailure(event);
                }
            }
            case "setuphelp" -> CommandSpace.handleSetupHelp(event);
            case "deletedata" -> CommandSpace.handleDeleteData(event, Objects.requireNonNull(event.getGuild()), member);
            case "setupserver" -> {
                assert member != null;
                CommandSpace.setupNewServer(event, event.getGuild(), member);
            }
            case "resetserver" -> CommandSpace.resetServerSetup(event);
            case "gradereport" -> {
                if (CommandSpace.apiFailure(event, member))
                    try {
                        assert member != null;
                        CommandSpace.getEnrollmentInfo(event, member);
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                else {
                    CommandSpace.sendAPIFailure(event);
                }
            }
            case "commandhelp" -> {
                CommandSpace.handleCommandHelp(event);
            }
            case "toggleCensor" -> {
                // currently un-Implemented!
            }
            case "createstudychannel" -> {
                // currently un-Implemented!
            }
            case "applycourseroles" -> {
                if (CommandSpace.apiFailure(event, member))
                    CommandSpace.applyCourseRoles(event, member, event.getGuild());
                else {
                    CommandSpace.sendAPIFailure(event);
                }
            }
            case "createcoursecategory" -> {
                sendCreationOptions(event, member, 0);
            }
            case "clearcourseroles" -> {
                CommandSpace.removeCourseRoles(event, member);
            }
            case  "deletecoursecategory" -> {
                CommandSpace.promptDeleteAll(event, event.getGuild(), event.getMember());
            }
            default -> {
            }
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.TEXT) && event.getChannel().getName().equals("verify-here")) {
            Member member = event.getMember();
            if (member != null && !member.isOwner() && !member.hasPermission(Permission.ADMINISTRATOR)
                    && !member.getRoles().contains(CommandSpace.serverManagerRole)) {
                event.getMessage().delete().queue();
            }
        }
    }




}
