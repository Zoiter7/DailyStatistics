package eu.gamesjap.Why;

import java.io.File;
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

import eu.gamesjap.Why.commands.ReloadCMD;
import eu.gamesjap.Why.discord.AppDiscord;
import eu.gamesjap.Why.discord.Task;
import eu.gamesjap.Why.files.ConfigFile;
import eu.gamesjap.Why.files.DataFile;
import eu.gamesjap.Why.files.MessageFile;
import org.spicord.Spicord;
import org.spicord.embed.Embed;
import org.spicord.embed.EmbedLoader;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class DailyStatistics extends Plugin {
	
	public static DailyStatistics plugin;
	private static int year = 0;
	private static int month = 0;
	private static int day = 0;
	public String prefix = "§aDailyStatistics §f» ";
	private static DailyStatistics instance;
	private DataFile data = new DataFile();
	public ConfigFile config = new ConfigFile();
	static ScheduledTask task;
	static Timer timer;
	public DailyStatistics() {
		instance = this;
	}
	  
	public static DailyStatistics getInstance() {
		return instance;
	}

	public void onEnable(){
		
		plugin = this;
		getProxy().getConsole().sendMessage("§aLoading DailyStatistics...");
		createDataFiles();
		collectInfo();

		if (Spicord.isLoaded()) { 
			Spicord.getInstance().getAddonManager().registerAddon(new AppDiscord());
		}else {
			getProxy().getConsole().sendMessage(prefix + "§cFailed to register Discord addon");
		}
		
		checkDTaskIsEnabled(false);
		
		getProxy().getPluginManager().registerCommand(this, new ReloadCMD("ds"));
		getProxy().getScheduler().schedule(this, () -> taskCollectInfo(false), 15, TimeUnit.SECONDS);
		
	}

	public void onDisable() {
		getProxy().getConsole().sendMessage("§cDisabling DailyStatistics...");
	}
	
	public void checkDTaskIsEnabled(boolean reload) {
		try {
			if(config.getConfig().getBoolean(("enable-dTask"))){
				if(reload) {
					prepareDiscordTask(true);
				}else {
					prepareDiscordTask(false);
				}
			}else if(reload) {
				task.cancel();
			}
		} catch (ParseException e) {
			getProxy().getConsole().sendMessage(prefix + "§cFailed to create discord task!");
			System.out.println(e.getMessage());
		}
	}
	
	public void createDataFiles() {
		data.createFile();
		config.createFile();
		MessageFile msgFile = new MessageFile();
		msgFile.createFile();
	}
	
	public void taskCollectInfo(boolean reload){

		if(reload) {
			task.cancel();
		}
		
		task = getProxy().getScheduler().schedule(DailyStatistics.plugin, new Runnable() {
		@Override
		public void run() {
			collectInfo();	
		}
		}, 0, config.getConfig().getInt("time"), TimeUnit.MINUTES);	
	}
	
	public void collectInfo() {
		
		List<String> serverList = config.getConfig().getStringList("servers");
		checkDate();
		String formatted = String.valueOf(day) + "-" + String.valueOf(month) + "-" + String.valueOf(year);

		int totalPlayersOnline = getProxy().getOnlineCount();
		getProxy().getConsole().sendMessage(prefix + "§aCollecting info about total online players right now §7(§c" + totalPlayersOnline + "§7)");
		
		
		for (int i = 0; i < serverList.size(); i++) {
			ServerInfo sv = getProxy().getServerInfo(serverList.get(i));
			if(sv == null) {
				break;
			}
			int online = sv.getPlayers().size();
			
			if(online != 0) {
				int actualSave = data.getData().getInt(formatted + "." + serverList.get(i) + ".maxOnline");
				
				if(online > actualSave) {
					data.getData().set(formatted + "." + serverList.get(i) + ".maxOnline", online);
					data.getData().set(formatted + "." + serverList.get(i) + ".maxTime", System.currentTimeMillis());
					data.saveData();
				}
			}	
		}
		
		if(totalPlayersOnline > data.getData().getInt(formatted + ".maxTotalOnline")) {
			data.getData().set(formatted + ".maxTotalOnline", totalPlayersOnline);
			data.getData().set(formatted + ".maxTotalOnlineTime", System.currentTimeMillis());
			data.saveData();
		}
	
	}
	
	public String getTime(long time) {
		
		Timestamp stamp = new Timestamp(time);
		Date date = new Date(stamp.getTime());
		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
		dateFormat.setTimeZone(TimeZone.getTimeZone(ZoneId.of(config.getConfig().getString("time-zone"))));
		
		return dateFormat.format(date);
	}
	
	public void checkDate() {
		
		Date d = new Date();
		LocalDate localDate = d.toInstant().atZone(ZoneId.of(config.getConfig().getString("time-zone"))).toLocalDate();
		int rMonth = localDate.getMonthValue();
		int rDay = localDate.getDayOfMonth();
		int rYear = localDate.getYear();
				
		if(year != rYear) {
			year = rYear;
		}
		
		if(month != rMonth) {
			month = rMonth;
		}
		
		if(day != rDay) {
			day = rDay;
		}
		
	}
	
	public void prepareDiscordTask(boolean reload) throws ParseException {
		
		if(reload) {
			if(timer != null) {
				timer.cancel();
			}
			task.cancel();
		}
		
	    DateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
	    dateFormatter.setTimeZone(TimeZone.getTimeZone(config.getConfig().getString("time-zone")));
	    
	    Date startDate = dateFormatter.parse(getActualDate() + " " + config.getConfig().getString("task-hour"));
	    if(System.currentTimeMillis() > dateFormatter.getCalendar().getTimeInMillis()){
	    	startDate = dateFormatter.parse(changeDate() + " " + config.getConfig().getString("task-hour")); 	
	    }
	    
	    getProxy().getConsole().sendMessage(prefix + "§aDiscord task will run at " + startDate);
	    timer = new Timer();
	    timer.schedule(new Task(), startDate);

	}
	
	public void prepareDiscordMessage(String date, boolean command, String cmdAction) {
		
		AppDiscord bot = AppDiscord.getInstance();
		
		if(command) {
			if(!data.getData().contains(date + ".maxTotalOnline")){
				String msg = "";
				if(cmdAction.equals("stat")) {
					msg = config.getConfig().getString("dMessage-noStatFound");
				}else if(cmdAction.equals("actual")) {
					msg = config.getConfig().getString("dMessage-noStatReady");
				}
				bot.executeMsg(null, msg);
				return;
			}
		}
		
		File folder = new File(plugin.getDataFolder() + File.separator + "messages");
		File file = new File(folder, "statMessage" + ".json");

		if(!file.exists()) {
			getProxy().getConsole().sendMessage(prefix + "§cERROR: The message file (statMessage.json) was not found.");
			return;
		}

		EmbedLoader loader = new EmbedLoader();
		loader.load(folder);

		Embed firstLoad = loader.getEmbedByName("statMessage");
		
		List<String> serverList = config.getConfig().getStringList("servers");
		
		String[] serverNames = new String[serverList.size()];
		Integer[] online = new Integer[serverList.size()];
		String[] time = new String[serverList.size()];
		
		for (int i = 0; i < serverList.size(); i++) {
			serverNames[i] = serverList.get(i);
			time[i] = getTime(data.getData().getLong(date + "." + serverList.get(i) + ".maxTime"));
			online[i] = data.getData().getInt(date + "." + serverList.get(i) + ".maxOnline");
		}
			
		String replace = firstLoad.toString();
		String replaced = "";

		for (int i = 0; i < serverNames.length; i++) {
			
			replaced = replace;
			if(firstLoad.toString().toString().contains("%server_name_" + i + "%")) {
				replace = replaced.toString().replace("%server_name_" + i + "%", String.valueOf(serverNames[i]));
				replaced = replace;
			}
			if(firstLoad.toString().toString().contains("%server_online_" + i + "%")) {
				 replace = replaced.replace("%server_online_" + i + "%", String.valueOf(online[i]));
				 replaced = replace;
			}
			if(firstLoad.toString().toString().toString().contains("%server_time_" + i + "%")) {
				replace = replaced.replace("%server_time_" + i + "%", (time[i]));
				replaced = replace;
			}	
		}
		
		
		if(replace.contains("%server_global_online%")) {
			replace = replaced.replace("%server_global_online%", String.valueOf(data.getData().getInt(date + ".maxTotalOnline")));
			replaced = replace;
		}
		
		if(replace.contains("%server_global_time%")){
			replace = replaced.replace("%server_global_time%", getTime(data.getData().getLong(date + ".maxTotalOnlineTime")));
			replaced = replace;
		}
		
		if(replace.contains("%date%")){
			replace = replaced.replace("%date%", date);
			replaced = replace;
		}
		
        Embed edited = Embed.fromJson(replace);
        bot.executeMsg(edited, null);
    
	}
		
	public String getActualDate() {

		return String.valueOf(day) + "-" + String.valueOf(month) + "-" + String.valueOf(year);	
	}
	
	public String changeDate() {

		Date d = new Date();
	    DateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
	    dateFormatter.setTimeZone(TimeZone.getTimeZone(config.getConfig().getString("time-zone")));
	    dateFormatter.setLenient(false);
	
		try {
			d = dateFormatter.parse(String.valueOf(day+1) + "-" + String.valueOf(month) + "-" + String.valueOf(year));
		} catch (ParseException e) {
			try {
				d = dateFormatter.parse(String.valueOf(day) + "-" + String.valueOf(month+1) + "-" + String.valueOf(year));
			} catch (ParseException e1) {
				try {
					d = dateFormatter.parse(String.valueOf(day) + "-" + String.valueOf(month) + "-" + String.valueOf(year+1));
				} catch (ParseException e2) {
					System.out.println("Error on change date! (This message not should show never)");
				}
			}
		}

		return dateFormatter.format(d).toString();
	}
	
	public void reload() {	
		config.reloadConfig();
		
		checkDTaskIsEnabled(true);	
		taskCollectInfo(true);
	}
	
}
