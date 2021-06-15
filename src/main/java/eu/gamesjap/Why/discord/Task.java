package eu.gamesjap.Why.discord;

import java.util.TimerTask;

import eu.gamesjap.Why.DailyStatistics;

public class Task extends TimerTask {

    private final DailyStatistics ds;

    public Task(DailyStatistics ds) {
        this.ds = ds;
    }

    public void run() {
        ds.prepareDiscordMessage(ds.getActualDate(), false, null);
        ds.getLogger().info(ds.prefix + "§aDay finished! (maybe?) Running discord task... (Sending message to your discord server)");
    }
}
