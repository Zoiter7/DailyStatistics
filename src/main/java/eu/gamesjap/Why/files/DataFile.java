package eu.gamesjap.Why.files;

import java.io.File;
import java.io.IOException;

import eu.gamesjap.Why.DailyStatistics;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class DataFile {

	private Configuration dataConfig;
	private static File dataFile;

	public void createFile() {

		File folder = DailyStatistics.plugin.getDataFolder();
		if(!folder.exists()) {
			folder.mkdir();
		}

		dataFile = new File(folder + File.separator +  "data" + ".yml");

		if(!dataFile.exists()) {
			try {
				dataFile.createNewFile();
			}catch(IOException e) {
				DailyStatistics.plugin.getProxy().getConsole().sendMessage("§CAn error has occurred when creating file...");

			}
		}  

		try {
			dataConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(dataFile);
			
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	public void saveData() {
		try {
			ConfigurationProvider.getProvider(YamlConfiguration.class).save(dataConfig, dataFile);

		}catch(IOException e) {

		}
	}

	public Configuration getData() {
		return dataConfig;
	}

}
