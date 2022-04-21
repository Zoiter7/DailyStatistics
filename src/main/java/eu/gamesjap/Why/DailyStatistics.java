package eu.gamesjap.Why;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import eu.gamesjap.Why.configuration.ConfigurationManager;
import eu.gamesjap.Why.configuration.DataManager;

import eu.gamesjap.Why.commands.ReloadCMD;
import eu.gamesjap.Why.configuration.SaveType;
import eu.gamesjap.Why.discord.DailyStatisticsDiscord;

import net.md_5.bungee.api.plugin.Plugin;
import org.spicord.SpicordLoader;

public class DailyStatistics extends Plugin {

    public final String prefix = "§aDailyStatistics §f» ";

    private Manager manager;
    private Settings settings;
    private DataManager saveHolder;

    private ConfigurationManager dataManager;
    private ConfigurationManager configManager;

    public DailyStatisticsDiscord addon;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();

        File configFile = saveFile(getDataFolder(), "config.yml");
        File dataFile   = saveFile(getDataFolder(), "data.yml");

        saveFile(new File(getDataFolder(), "messages"), "statMessage.json");

        this.dataManager   = new ConfigurationManager(dataFile, getLogger());
        this.configManager = new ConfigurationManager(configFile, getLogger());

        getLogger().info("§aLoading DailyStatistics...");

        manager = new Manager(this);
        settings = new Settings(this);

        SaveType saveType = SaveType.YAML;
        if (configManager.getConfig().getString("saving-method").equalsIgnoreCase("SQL")) {
            saveType = SaveType.SQL;
        }
        saveHolder = new DataManager(this, dataManager, saveType);

        this.addon = new DailyStatisticsDiscord(this);

        SpicordLoader.addStartupListener(spicord -> spicord.getAddonManager().registerAddon(addon));

        getProxy().getPluginManager().registerCommand(this, new ReloadCMD(this));

    }

    @Override
    public void onDisable() {
        getLogger().info("§cDisabling DailyStatistics...");
    }

    public Manager getManager(){
        return manager;
    }

    public Settings getSettings(){
        return settings;
    }

    public DataManager getDataManager() {
        return saveHolder;
    }

    public void reload() {
        configManager.reloadConfig();
        settings.reload();
    }

    public File saveFile(File folder, String file) {
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

    public ConfigurationManager getConfigManager() {
        return configManager;
    }
}
