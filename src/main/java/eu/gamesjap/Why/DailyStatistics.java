package eu.gamesjap.Why;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.spicord.SpicordLoader;
import org.spicord.embed.Embed;
import org.spicord.embed.EmbedLoader;

import eu.gamesjap.Why.commands.ReloadCMD;
import eu.gamesjap.Why.discord.DailyStatisticsDiscord;
// import eu.gamesjap.Why.discord.Task;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class DailyStatistics extends Plugin {

    private static int year = 0;
    private static int month = 0;
    private static int day = 0;

    public final String prefix = "§aDailyStatistics §f» ";

    private ConfigurationManager dataManager;
    private ConfigurationManager configManager;

    private DailyStatisticsDiscord addon;

    static ScheduledTask task;
    static Timer timer;

    private static ScheduledTask dailystatstask;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();

        File configFile = saveFile(getDataFolder(), "config.yml");
        File dataFile   = saveFile(getDataFolder(), "data.yml");

        saveFile(new File(getDataFolder(), "messages"), "statMessage.json");

        this.dataManager   = new ConfigurationManager(dataFile, getLogger());
        this.configManager = new ConfigurationManager(configFile, getLogger());

        getLogger().info("§aLoading DailyStatistics...");

        collectInfo();

        this.addon = new DailyStatisticsDiscord(this);

        SpicordLoader.addStartupListener(spicord -> {
            spicord.getAddonManager().registerAddon(addon);
        });

        checkDTaskIsEnabled(false);

        getProxy().getPluginManager().registerCommand(this, new ReloadCMD(this));
        getProxy().getScheduler().schedule(this, () -> taskCollectInfo(false), 15, TimeUnit.SECONDS);

    }

    @Override
    public void onDisable() {
        getLogger().info("§cDisabling DailyStatistics...");
    }

    public ConfigurationManager getDataManager() {
        return dataManager;
    }

    public ConfigurationManager getConfigManager() {
        return configManager;
    }

    public void checkDTaskIsEnabled(boolean reload) {
        try {
            if (configManager.getConfig().getBoolean(("enable-dTask"))) {
                if (reload) {
                    prepareDiscordTask(true);
                } else {
                    prepareDiscordTask(false);
                }
            } else if (reload) {
                task.cancel();
            }
        } catch (ParseException e) {
            getLogger().info(prefix + "§cFailed to create discord task!");
            System.out.println(e.getMessage());
        }
    }

    public void taskCollectInfo(boolean reload) {

        if (reload) {
            task.cancel();
        }

        task = getProxy().getScheduler().schedule(this, new Runnable() {
            @Override
            public void run() {
                collectInfo();
            }
        }, 0, configManager.getConfig().getInt("time"), TimeUnit.MINUTES);
    }

    public void collectInfo() {

        List<String> serverList = configManager.getConfig().getStringList("servers");
        checkDate();
        String formatted = String.valueOf(day) + "-" + String.valueOf(month) + "-" + String.valueOf(year);

        int totalPlayersOnline = getProxy().getOnlineCount();
        getLogger().info(
                prefix + "§aCollecting info about total online players right now §7(§c" + totalPlayersOnline + "§7)");

        for (int i = 0; i < serverList.size(); i++) {
            ServerInfo sv = getProxy().getServerInfo(serverList.get(i));
            if (sv == null) {
                break;
            }
            int online = sv.getPlayers().size();

            if (online != 0) {
                int actualSave = dataManager.getConfig().getInt(formatted + "." + serverList.get(i) + ".maxOnline");

                if (online > actualSave) {
                    dataManager.getConfig().set(formatted + "." + serverList.get(i) + ".maxOnline", online);
                    dataManager.getConfig().set(formatted + "." + serverList.get(i) + ".maxTime", System.currentTimeMillis());
                    dataManager.saveConfig();
                }
            }
        }

        if (totalPlayersOnline > dataManager.getConfig().getInt(formatted + ".maxTotalOnline")) {
            dataManager.getConfig().set(formatted + ".maxTotalOnline", totalPlayersOnline);
            dataManager.getConfig().set(formatted + ".maxTotalOnlineTime", System.currentTimeMillis());
            dataManager.saveConfig();
        }

    }

    public String getTime(long time) {

        Timestamp stamp = new Timestamp(time);
        Date date = new Date(stamp.getTime());
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        dateFormat.setTimeZone(TimeZone.getTimeZone(ZoneId.of(configManager.getConfig().getString("time-zone"))));

        return dateFormat.format(date);
    }

    public void checkDate() {

        Date d = new Date();
        LocalDate localDate = d.toInstant().atZone(ZoneId.of(configManager.getConfig().getString("time-zone"))).toLocalDate();
        int rMonth = localDate.getMonthValue();
        int rDay = localDate.getDayOfMonth();
        int rYear = localDate.getYear();

        if (year != rYear) {
            year = rYear;
        }

        if (month != rMonth) {
            month = rMonth;
        }

        if (day != rDay) {
            day = rDay;
        }

    }

    public void prepareDiscordTask(boolean reload) throws ParseException {

        if (reload) {
            if (timer != null) {
                timer.cancel();
            }
            task.cancel();
            dailystatstask.cancel();
        }

        DateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        dateFormatter.setTimeZone(TimeZone.getTimeZone(configManager.getConfig().getString("time-zone")));

        Date startDate = dateFormatter.parse(getActualDate() + " " + configManager.getConfig().getString("task-hour"));
        if (System.currentTimeMillis() > dateFormatter.getCalendar().getTimeInMillis()) {
            startDate = dateFormatter.parse(changeDate() + " " + configManager.getConfig().getString("task-hour"));
        }

        getLogger().info(prefix + "§aDiscord task will run at " + startDate);
        // timer = new Timer();
        // timer.schedule(new Task(this), startDate);
        Date now = java.util.Calendar.getInstance().getTime();
        long startmiliseconds = (startDate.getTime() - now.getTime());
        ScheduledTask T = getProxy().getScheduler().schedule(this, () -> prepareDiscordMessage(getActualDate(), false, null), startmiliseconds, 24*60*60L*1000L, TimeUnit.MILLISECONDS);
        dailystatstask = T;
    }

    public void prepareDiscordMessage(String date, boolean command, String cmdAction) {

        if (command) {
            if (!dataManager.getConfig().contains(date + ".maxTotalOnline")) {
                String msg = "";
                if (cmdAction.equals("stat")) {
                    msg = configManager.getConfig().getString("dMessage-noStatFound");
                } else if (cmdAction.equals("actual")) {
                    msg = configManager.getConfig().getString("dMessage-noStatReady");
                }
                addon.executeMsg(null, msg);
                return;
            }
        }

        File folder = new File(getDataFolder(), "messages");
        File file = new File(folder, "statMessage.json");

        if (!file.exists()) {
            getLogger().info(prefix + "§cERROR: The message file (statMessage.json) was not found.");
            return;
        }

        EmbedLoader loader = new EmbedLoader();
        loader.load(folder);

        Embed firstLoad = loader.getEmbedByName("statMessage");

        List<String> serverList = configManager.getConfig().getStringList("servers");

        String[] serverNames = new String[serverList.size()];
        Integer[] online = new Integer[serverList.size()];
        String[] time = new String[serverList.size()];

        for (int i = 0; i < serverList.size(); i++) {
            serverNames[i] = serverList.get(i);
            time[i] = getTime(dataManager.getConfig().getLong(date + "." + serverList.get(i) + ".maxTime"));
            online[i] = dataManager.getConfig().getInt(date + "." + serverList.get(i) + ".maxOnline");
        }

        String replace = firstLoad.toString();
        String replaced = "";

        for (int i = 0; i < serverNames.length; i++) {

            replaced = replace;
            if (firstLoad.toString().toString().contains("%server_name_" + i + "%")) {
                replace = replaced.toString().replace("%server_name_" + i + "%", String.valueOf(serverNames[i]));
                replaced = replace;
            }
            if (firstLoad.toString().toString().contains("%server_online_" + i + "%")) {
                replace = replaced.replace("%server_online_" + i + "%", String.valueOf(online[i]));
                replaced = replace;
            }
            if (firstLoad.toString().toString().toString().contains("%server_time_" + i + "%")) {
                replace = replaced.replace("%server_time_" + i + "%", (time[i]));
                replaced = replace;
            }
        }

        if (replace.contains("%server_global_online%")) {
            replace = replaced.replace("%server_global_online%",
                    String.valueOf(dataManager.getConfig().getInt(date + ".maxTotalOnline")));
            replaced = replace;
        }

        if (replace.contains("%server_global_time%")) {
            replace = replaced.replace("%server_global_time%",
                    getTime(dataManager.getConfig().getLong(date + ".maxTotalOnlineTime")));
            replaced = replace;
        }

        if (replace.contains("%date%")) {
            replace = replaced.replace("%date%", date);
            replaced = replace;
        }

        Embed edited = Embed.fromJson(replace);
        addon.executeMsg(edited, null);

    }

    public String getActualDate() {

        return String.valueOf(day) + "-" + String.valueOf(month) + "-" + String.valueOf(year);
    }

    public String changeDate() {

        Date d = new Date();
        DateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
        dateFormatter.setTimeZone(TimeZone.getTimeZone(configManager.getConfig().getString("time-zone")));
        dateFormatter.setLenient(false);

        try {
            d = dateFormatter.parse(String.valueOf(day + 1) + "-" + String.valueOf(month) + "-" + String.valueOf(year));
        } catch (ParseException e) {
            try {
                d = dateFormatter
                        .parse(String.valueOf(day) + "-" + String.valueOf(month + 1) + "-" + String.valueOf(year));
            } catch (ParseException e1) {
                try {
                    d = dateFormatter
                            .parse(String.valueOf(day) + "-" + String.valueOf(month) + "-" + String.valueOf(year + 1));
                } catch (ParseException e2) {
                    System.out.println("Error on change date! (This message not should show never)");
                }
            }
        }

        return dateFormatter.format(d).toString();
    }

    public void reload() {
        configManager.reloadConfig();

        checkDTaskIsEnabled(true);
        taskCollectInfo(true);
    }

    public static File saveFile(File folder, String file) {
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File outFile = new File(folder, file);
        if (!outFile.exists()) {
            InputStream in = DailyStatistics.class.getResourceAsStream("/" + file);
            try {
                if (in == null) {
                    outFile.createNewFile();
                } else {
                    Files.copy(in, outFile.toPath());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return outFile;
    }
}
