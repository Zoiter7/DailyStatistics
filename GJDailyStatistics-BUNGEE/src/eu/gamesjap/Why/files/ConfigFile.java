package eu.gamesjap.Why.files;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import eu.gamesjap.Why.DailyStatistics;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class ConfigFile {

	private Configuration configConf;
	private static File configFile;

	public void createFile() {

		File folder = DailyStatistics.plugin.getDataFolder();
		if(!folder.exists()) {
			folder.mkdir();
		}

		configFile = new File(folder + File.separator +  "config" + ".yml");

		if(!configFile.exists()) {
            try {
            	InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml");
                Files.copy(in, configFile.toPath());
			}catch(IOException e) {
				DailyStatistics.plugin.getProxy().getConsole().sendMessage("§CAn error has occurred when creating file...");
			}
		}  

		try {
			configConf = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	public void saveConfig() {
		try {
			ConfigurationProvider.getProvider(YamlConfiguration.class).save(configConf, configFile);

		}catch(IOException e) {

		}
	}

	public Configuration getConfig() {
		return configConf;
	}

}
