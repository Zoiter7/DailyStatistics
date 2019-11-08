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

import eu.gamesjap.Why.discord.AppDiscord;
import eu.gamesjap.Why.discord.Task;
import eu.gamesjap.Why.files.ConfigFile;
import eu.gamesjap.Why.files.DataFile;
import eu.gamesjap.Why.files.MessageFile;
import eu.mcdb.spicord.Spicord;
import eu.mcdb.spicord.embed.Embed;
import eu.mcdb.spicord.embed.EmbedLoader;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;


public class DailyStatistics extends Plugin {
	
	public static DailyStatistics plugin;
	private static int year = 0;
	private static int month = 0;
	private static int day = 0;
	public String prefix = "§aDailyStatistics §f» ";
	private static DailyStatistics instance;
	private DataFile data = new DataFile();
	public ConfigFile config = new ConfigFile();

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

			try {
				if(config.getConfig().getBoolean(("enable-dTask"))){
					prepareDiscordTask();
				}
			} catch (ParseException e) {
				getProxy().getConsole().sendMessage(prefix + "Failed to create discord task!");
				System.out.println(e.getMessage());
			}

		}else {
			getProxy().getConsole().sendMessage(prefix + "§cFailed to register Discord addon");
		}
		
		
		getProxy().getScheduler().schedule(this, () -> taskCollectInfo(), 15, TimeUnit.SECONDS);
		
	}

	public void onDisable() {
		getProxy().getConsole().sendMessage("§cDisabling DailyStatistics...");
	}
	
	public void createDataFiles() {
		data.createFile();
		config.createFile();
		MessageFile msgFile = new MessageFile();
		msgFile.createFile();
	}
	
	public void taskCollectInfo(){
				
		getProxy().getScheduler().schedule(DailyStatistics.plugin, new Runnable() {
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
	/*	
		if(startup) {
			if(data.getData().getInt(formatted + ".online") == 0) {
				data.getData().set(formatted + ".online", 0);
				data.saveData();
				return;
			}
		}
*/
		int totalPlayersOnline = getProxy().getOnlineCount();
		getProxy().getConsole().sendMessage("§fDailyStatistics » §aCollecting info about total online players right now §7(§c" + totalPlayersOnline + "§7)");
		
		
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
	
	public void prepareDiscordTask() throws ParseException {
		
	    DateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
	    dateFormatter.setTimeZone(TimeZone.getTimeZone(config.getConfig().getString("time-zone")));
	    Date startDate = dateFormatter.parse(getActualDate() + " " + config.getConfig().getString("task-hour"));
	    getProxy().getConsole().sendMessage(prefix + "§aDiscord task will run at " + startDate);
	    Timer timer = new Timer();
	    timer.schedule(new Task(), startDate);
	}
	
	public void prepareDiscordMessage(String date, boolean command) {
		
		AppDiscord bot = AppDiscord.getInstance();
		
		if(command) {
			if(!data.getData().contains(date + ".maxTotalOnline")){
				bot.executeMsg(null, config.getConfig().getString("dMessage-noStatFound"));
				return;
			}
		}
		
		File folder = new File(plugin.getDataFolder() + File.separator + "messages");
		File file = new File(folder, "statMessage" + ".json");

		if(!file.exists()) {
			getProxy().getConsole().sendMessage(prefix + "§cERROR: The messages file (statMessage.json) was not found.");
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
	
	public String replace(String originalStr, String remplace, String newR) {
		
		String finalStr = originalStr.replace(remplace, newR);
		
		return finalStr;
	}
	
	public String getActualDate() {
		
		return String.valueOf(day) + "-" + String.valueOf(month) + "-" + String.valueOf(year);
		
	}
}
