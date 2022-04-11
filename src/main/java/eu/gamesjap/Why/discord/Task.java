package eu.gamesjap.Why.discord;

import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import eu.gamesjap.Why.DailyStatistics;

/**
 * @deprecated
 */
public class Task extends TimerTask {

    private final DailyStatistics ds;

    public Task(DailyStatistics ds) {
        this.ds = ds;
    }

    public void run() {
        // ds.prepareDiscordMessage(ds.getActualDate(), false, null);
        ds.getLogger().info(ds.prefix + "§aDay finished! (maybe?) Running discord task... (Sending message to your discord server)");
        ds.getProxy().getScheduler().schedule(ds, () -> ds.prepareDiscordMessage(ds.getActualDate(), false, null), 0, 1, TimeUnit.DAYS);

    }
}
