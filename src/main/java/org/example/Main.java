package org.example;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;

import javax.security.auth.login.LoginException;

public class Main {

    private final ShardManager shardManager;
    private final Dotenv config;

    public Main() throws LoginException {
        config = Dotenv.configure().ignoreIfMissing().load();
        String token = config.get("TOKEN");
        String NowPlaying = "Femboy Besties";
        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(token);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.playing(NowPlaying));
        builder.enableIntents(GatewayIntent.MESSAGE_CONTENT);
        shardManager = builder.build();

        shardManager.addEventListener(new MyListener());
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
