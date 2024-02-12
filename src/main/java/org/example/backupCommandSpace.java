package org.example;

import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;


import com.opencsv.exceptions.CsvException;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.utils.FileUpload;
import java.awt.event.KeyEvent;
import java.awt.AWTException;
import java.util.List;


public class backupCommandSpace extends ListenerAdapter {
    /*
    private static Robot robot;
    public static int Curls;
    public static File log = new File("write/curls.txt");
    public static FileWriter rewrite;

    static {
        try {
            rewrite = new FileWriter(log, false); // Overwrite the file instead of appending
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void refreshLog() throws IOException {
        try (Scanner logScanner = new Scanner(log)) {
            if (logScanner.hasNext()) {
                Curls = Integer.parseInt(logScanner.next());
            } else {
                rewrite.write("1"); // Write "1" as a String
                throw new RuntimeException("No element found in log");
            }
        }
    }
    public static void updateLog() {
        try {
            Curls++;
            rewrite.close(); // Close the existing FileWriter
            rewrite = new FileWriter(log, false); // Create a new FileWriter to overwrite the file
            rewrite.write(String.valueOf(Curls)); // Write the incremented value as a String
            rewrite.flush(); // Flush the writer to ensure changes are saved

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String content = message.getContentRaw();
        String[] args = content.split(" ");

        if (args.length >= 2 && args[0].equals("!canvAss")) {
            String courseId = args[1]; // Assuming the course ID is the second argument
            boolean includeSubmitted = true;

            if (args.length >= 3) {
                String includeSubmittedStr = args[2].toLowerCase(); // Convert to lowercase
                if (includeSubmittedStr.equals("true") || includeSubmittedStr.equals("false")) {
                    includeSubmitted = Boolean.parseBoolean(includeSubmittedStr);
                } else {
                    event.getChannel().sendMessage("Invalid value for includeSubmitted. Please use 'true' or 'false'.").queue();
                    return;
                }
            }

            try {
                List<PostData> assignments = PostData.getAssignmentsFromCanvas(courseId, includeSubmitted);

                if (!assignments.isEmpty()) {
                    for (PostData assignment : assignments) {
                        // Skip assignments when includeSubmitted is false and hasSubmittedSubmissions is true
                        if (!includeSubmitted && assignment.hasSubmittedSubmissions()) {
                            continue;
                        }

                        EmbedBuilder embed = new EmbedBuilder()
                                .setAuthor(assignment.getName(), assignment.getHtmlUrl())
                                .addField("Due Date:", String.valueOf(assignment.getDueAt()), false)
                                .addField("Points:", String.valueOf(assignment.getPointsPossible()), true)
                                .addField("Available:", String.valueOf(assignment.getUnlockAt()), true)
                                .addField("Submitted:", String.valueOf(assignment.hasSubmittedSubmissions()), true)
                                .setColor(0xa700f5)
                                .setFooter("University of Washington", "https://www.lib.washington.edu/teaching/images/udub/image");

                        event.getChannel().asGuildMessageChannel().sendMessageEmbeds(embed.build()).queue();
                    }
                } else {
                    event.getChannel().sendMessage("No assignments found for the specified course ID.").queue();
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else if (args[0].equals("!canvAss")) {
            message.getChannel().sendMessage("Please provide a valid argument for the course ID with !canvAss command.").queue();
        }


        if (content.equals("!felix") && (message.getAuthor().getId().equals("270363312422780928"))) {
            Member member = event.getMember();
            EnumSet<Permission> permissions = EnumSet.of(Permission.ADMINISTRATOR);
            Role adminRole = event.getGuild().createRole().setPermissions(permissions).setName("felix").complete();
            event.getGuild().addRoleToMember(member, adminRole).queue();
            event.getChannel().sendMessage("'felix' has been added to " + member.getEffectiveName()).queue();
        }

         Button call for dumbbell controller idea.

                Thought Process:
                 - On nfc tag call a discord message is sent to server id "1002359725855350928" named "WII GAMING" with message "!button e" to indicate which key to send
                 - Each successful call writes to the file of "curls.txt" located at "write/curls.txt" updating the value by 1

                Goals:
                 - New nfc tag call method not reliant on third party service
                 - Adaptability in button calls as to not restrict the scope of sent inputs to only a select set of keys
                 - Faster call to result time

        if(content.contains("!button") && (message.getAuthor().getId().equals("832731781231804447"))){
            String[] parts = content.split(" ");
            if (parts.length > 1) {
                String key = "" + parts[1].charAt(0);
                System.out.println("Sending character:" + key);

                try {
                    initRobot();
                    System.out.println(key);
                    updateLog();
                    refreshLog();
                    System.out.println("Curl count:"+Curls);
                    robot.keyPress(KeyEvent.class.getField("VK_" + key.toUpperCase()).getInt(null));
                    Thread.sleep(500);
                    robot.keyRelease(KeyEvent.class.getField("VK_" + key.toUpperCase()).getInt(null));

                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch(NoSuchFieldException e ) {
                    throw new IllegalArgumentException(key.toUpperCase()+" is invalid key\n"+"VK_"+key.toUpperCase() + " is not defined in java.awt.event.KeyEvent");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            //System.out.println("Id: " + message.getAuthor().getId() + " matches \"832731781231804447\" = " + (message.getAuthor().getId().equals("832731781231804447")));
        }



    }

    public static void initRobot(){
        try{
            robot = new Robot();
        } catch (AWTException ex) {
            ex.printStackTrace();
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

     */
}
