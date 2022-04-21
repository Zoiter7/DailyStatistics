package eu.gamesjap.Why.configuration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class ConfigurationManager {

    private final Logger logger;
    private final File file;

    private final ConfigurationProvider provider;

    private Configuration configuration;

    public ConfigurationManager(File configFile, Logger logger) {
        this.file = configFile;
        this.logger = logger;
        this.provider = ConfigurationProvider.getProvider(YamlConfiguration.class);

        this.loadFile();
    }

    public void loadFile() {
        try {
            configuration = provider.load(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveConfig() {
        try {
            provider.save(configuration, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        try {
            configuration = provider.load(file);
        } catch (IOException e) {
            logger.severe("An error has occurred when reloading the file!");
            e.printStackTrace();
        }
    }

    public Configuration getConfig() {
        return configuration;
    }
}
