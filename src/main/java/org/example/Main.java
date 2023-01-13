package org.example;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;

import javax.security.auth.login.LoginException;

public class Main {

    private final ShardManager shardManager;

    public Main() throws LoginException {
        String NowPlaying = "Femboy Besties";
        String Token = "MTA0OTc5OTg1NDYzNDg0MDA5NA.Ga-L2K.oOeTWZWuHe2Dj1baVprVvDWnUg7u8iBH9EYeH8";
        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(Token);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.playing(NowPlaying));
        shardManager = builder.build();
    }

    public ShardManager getShardManager() {
        return shardManager;
    }

    public static void main(String[] args) {
        try {
            Main bot = new Main();
        } catch (LoginException e){
            System.out.println("Invalid Token! FIX IT");
        }

    }
}
