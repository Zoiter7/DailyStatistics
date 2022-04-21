package eu.gamesjap.Why;

import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class Settings {

    private final DailyStatistics ds;
    private ScheduledTask collectDataTask, discordMsgTask;

    private int COLLECTING_TIME;
    private String TIME_ZONE;
    private String TASK_HOUR;

    public Settings(DailyStatistics ds){
        this.ds = ds;

        COLLECTING_TIME = ds.getConfigManager().getConfig().getInt("collecting-time");
        TIME_ZONE = ds.getConfigManager().getConfig().getString("time-zone");
        TASK_HOUR = ds.getConfigManager().getConfig().getString("task-hour");

        start();
    }

    //Start tasks
    private void start(){
        if (ds.getConfigManager().getConfig().getBoolean(("enable-discordTask"))) {
            if(ds.getConfigManager().getConfig().getString("discord-channelID").isEmpty()){
                ds.getLogger().severe("Discord task cannot be created because \"discord-channelID\" is empty!");
            }else{
                try {
                    prepareDiscordTask();
                } catch (ParseException e) {
                    ds.getLogger().severe("Error while setting discord task.\n" + e.getMessage());
                }
            }
        }

        collectDataTask = ds.getProxy().getScheduler().schedule(ds, () ->
                        ds.getManager().collectInfo(),
                1, COLLECTING_TIME, TimeUnit.MINUTES);

    }

    private void prepareDiscordTask() throws ParseException {
        if (!TIME_ZONE.equalsIgnoreCase("default")){
            try {
                ZoneId.of(TIME_ZONE);
                try{
                    TimeZone.setDefault(TimeZone.getTimeZone(TIME_ZONE));
                }catch (SecurityException e){
                    ds.getLogger().severe(e.getMessage());
                }
            } catch (Exception e) {
                ds.getLogger().severe("The TimeZone " + TIME_ZONE + " not exists! Plugin will use the default system TimeZone");
            }
        }

        DateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");

        if(!TASK_HOUR.equalsIgnoreCase("default")){
            Date startDate = dateFormatter.parse(getActualDate() + " " + TASK_HOUR);;

            if (System.currentTimeMillis() >= dateFormatter.getCalendar().getTimeInMillis()) {
                //Setup task for next day
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(startDate.getTime());
                c.add(Calendar.DATE, 1);
                startDate = c.getTime();
            }

            Date now = Calendar.getInstance().getTime();
            long startMilliseconds = startDate.getTime() - now.getTime();

            ds.getLogger().info("§aDiscord task will run on " + startDate);

            discordMsgTask = ds.getProxy().getScheduler().schedule(ds, () ->
                    ds.getManager().prepareDiscordMessage(getActualDate(), false, null),
                    startMilliseconds, 24*60*60L*1000L, TimeUnit.MILLISECONDS);

        }else{
            //If hour is not configured will send message everyday since server started.
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE, 1);

            ds.getLogger().info("§aDiscord task will run on " + dateFormatter.format(c.getTime()));

            discordMsgTask = ds.getProxy().getScheduler().schedule(ds, () ->
                    ds.getManager().prepareDiscordMessage(getActualDate(), false, null),
                    1, 1, TimeUnit.DAYS);
        }
    }


    public String getTimeFromTimestamp(long time) {
        if(time == 0) return "0";

        Timestamp stamp = new Timestamp(time);
        Date date = new Date(stamp.getTime());
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");

        //if (!TIME_ZONE.contentEquals("default")) dateFormat.setTimeZone(TimeZone.getTimeZone(ZoneId.of(TIME_ZONE)));

        return dateFormat.format(date);
    }


    /*
    public void checkDate() {
        Date d = new Date();
        LocalDate localDate = d.toInstant().atZone(ZoneId.of(TIME_ZONE)).toLocalDate();
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
*/
    public String getActualDate() {
        LocalDate t = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        /*
        if(TIME_ZONE.equalsIgnoreCase("default")){
            t = new Date().toInstant().atZone(TimeZone.getDefault().toZoneId())).toLocalDate();
        }else{
            t = new Date().toInstant().atZone(ZoneId.of(TIME_ZONE)).toLocalDate();
        }
*/
        return t.getDayOfMonth() + "-" + t.getMonthValue() + "-" + t.getYear();
    }
/*
    public String changeDate() {
        Date d = new Date();
        DateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
        dateFormatter.setTimeZone(TimeZone.getTimeZone(ds.getConfigManager().getConfig().getString("time-zone")));
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

        return dateFormatter.format(d);
    }
*/

    public void reload(){
        if(collectDataTask != null) collectDataTask.cancel();
        if(discordMsgTask != null) discordMsgTask.cancel();

        COLLECTING_TIME = ds.getConfigManager().getConfig().getInt("collecting-time");
        TIME_ZONE = ds.getConfigManager().getConfig().getString("time-zone");
        TASK_HOUR = ds.getConfigManager().getConfig().getString("task-hour");

        start();
    }

}
