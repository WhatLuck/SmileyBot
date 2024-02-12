package org.example;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import static org.example.MyListener.registerCommands;

public class Main {

    private final ShardManager shardManager;

    private final Dotenv config;

    public Main() throws LoginException {
        config = Dotenv.configure().ignoreIfMissing().load();
        String token = config.get("TOKEN");
        String NowPlaying = "test";
        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(token);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.playing(NowPlaying));
        builder.enableIntents(GatewayIntent.MESSAGE_CONTENT);
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS);
        shardManager = builder.build();
        shardManager.addEventListener(new MyListener());

        MyListener myListener = new MyListener();
        shardManager.addEventListener(new ListenerAdapter() {
            @Override
            public void onReady(ReadyEvent event) {
                myListener.onReady(event);
            }
        });

        shardManager.addEventListener(new ListenerAdapter() {
            @Override
            public void onGuildJoin(GuildJoinEvent event) {
                myListener.onGuildJoin(event);
            }
        });
    }

    public ShardManager getShardManager() {
        return shardManager;
    }

    public Dotenv getConfig() { return config; }

    public static void main(String[] args) {

        try {
            Main bot = new Main();
        } catch (LoginException e){
            System.out.println("Invalid Token! FIX IT");
        }

    }
}
