package eu.gamesjap.Why.discord;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import eu.gamesjap.Why.DailyStatistics;
import eu.mcdb.spicord.api.addon.SimpleAddon;
import eu.mcdb.spicord.bot.DiscordBot;
import eu.mcdb.spicord.bot.command.DiscordBotCommand;
import eu.mcdb.spicord.embed.Embed;
import eu.mcdb.spicord.embed.EmbedSender;
import net.dv8tion.jda.core.entities.TextChannel;

public class AppDiscord extends SimpleAddon {

	private static AppDiscord instance;
	private DiscordBot bot;
	
    public static AppDiscord getInstance() {
        return instance;
    }
    
    public AppDiscord() {
        super(
            "DailyStatistics",
            "daily_statistics",
            "Zoiter7"
        );
        
        instance = this;
    }

    @Override
    public void onLoad(DiscordBot bot) {
   	
    	this.bot = bot;  
    	
    	bot.onCommand("stat", this::statsCmd);
    }
    
    private void statsCmd(DiscordBotCommand command) {
    	
    	String[] arguments = command.getArguments();
	        
    	if(arguments.length > 0) {
    		if(!arguments[0].isEmpty() && arguments[0].contentEquals("actual")) {
    			DailyStatistics.getInstance().prepareDiscordMessage(DailyStatistics.getInstance().getActualDate(), true, "actual");

    		}else if (!arguments[0].isEmpty()){
    			DateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
    			try {
    				dateFormatter.setLenient(false);
    				dateFormatter.parse(arguments[0]);
    			} catch (ParseException e) {
    				this.executeMsg(null, DailyStatistics.getInstance().config.getConfig().getString("dMessage-incorrectDataFormat"));
    				return;
    			}
    			String date = arguments[0].toString();

    			if(date != null) {
    				DailyStatistics.getInstance().prepareDiscordMessage(date, true, "stat");
            		
            	}
    		}

    	}else {
    		executeMsg(null, ">>> **Commands:** \n" + 
    				" -stat <date>\n" + 
    				" -stat actual");
    	}
    	
    }
    
    
    public void executeMsg(Embed msg, String normalMsg) {
    	
		TextChannel channel = bot.getJda().getTextChannelById(DailyStatistics.getInstance().config.getConfig().getString("discord-channelID"));
		
		if(normalMsg != null) {
			channel.sendMessage(normalMsg).queue();
		}else {
			EmbedSender.prepare(channel, msg).queue();
		}
    }

}