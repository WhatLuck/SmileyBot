package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
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

    public static void registerCommands(Guild guild) {
        CommandListUpdateAction commands = guild.updateCommands();
        commands.addCommands(
                Commands.slash("setapi", "Set user API key")
                        .addOption(OptionType.STRING, "apikey", "The API key")
                        .addOption(OptionType.STRING, "platform", "Your canvas based website url"),
                Commands.slash("getdata", "Get user data"),
                Commands.slash("todo", "Finds the user's todo assignments"),
                Commands.slash("setuphelp", "Help page for user profiling"),
                Commands.slash("deletedata", "Removes user data from database")
                ).queue();
    }

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

    private final String JDBC_DATABASE_URL = "jdbc:sqlite:database.db";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        try (Connection connection = DriverManager.getConnection(JDBC_DATABASE_URL)) {
            CommandSpace.createTableIfNotExists(connection);
        } catch (SQLException e) {
            e.printStackTrace();
            event.reply("Error occurred while accessing the database.").setEphemeral(true).queue();
            return;
        }

        switch (event.getName()) {
            case "setapi":
                CommandSpace.handleSetAPI(event);
                break;
            case "getdata":
                CommandSpace.handleGetData(event);
                break;
            case "todo":
                CommandSpace.handleTodo(event);
                break;
            case "setuphelp":
                CommandSpace.handleSetupHelp(event);
                break;
            case "deletedata":
                CommandSpace.handleDeleteData(event);
                break;
            default:
                break;
        }

    }





}
