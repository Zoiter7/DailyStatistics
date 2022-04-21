package eu.gamesjap.Why.configuration;

import eu.gamesjap.Why.DailyStatistics;
import eu.gamesjap.Why.model.ServerData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataManager {

    private final DailyStatistics ds;
    private final ConfigurationManager config;
    private final SaveType savingMethod;

    public DataManager(DailyStatistics ds, ConfigurationManager configManager, SaveType saveType){
        this.ds = ds;
        this.config = configManager;
        this.savingMethod = saveType;
    }

    public void saveData(Map<String, Integer> servers, int globalOnline, String actualDate){
        //YAML:
        for (Map.Entry<String, Integer> server : servers.entrySet()) {
            if(savingMethod == SaveType.YAML){
                config.getConfig().set(actualDate + "." + server.getKey() + ".maxOnline", server.getValue());
                config.getConfig().set(actualDate + "." + server.getKey() + ".maxTime", System.currentTimeMillis());
            }else{
                //sql
            }

        }

        if(savingMethod == SaveType.YAML){
            if (globalOnline > getMaxGlobalOnline(actualDate)) {
                config.getConfig().set(actualDate + ".maxTotalOnline", globalOnline);
                config.getConfig().set(actualDate + ".maxTotalOnlineTime", System.currentTimeMillis());
            }

            config.saveConfig();
        }
    }

    public List<ServerData> getData(String date){
        final List<String> serverList = ds.getConfigManager().getConfig().getStringList("servers");
        List<ServerData> list = new ArrayList<>();

        if(savingMethod == SaveType.YAML){
            for (int i = 0; i < serverList.size(); i++) {
                String name = serverList.get(i);
                int online = config.getConfig().getInt(date + "." + serverList.get(i) + ".maxOnline");
                String time = ds.getSettings().getTimeFromTimestamp(config.getConfig().getLong(date + "." + serverList.get(i) + ".maxTime"));

                list.add(new ServerData(name, online, time));
            }

            list.add(new ServerData("7global7", getMaxGlobalOnline(date), getMaxGlobalTime(date)));
        }else{

        }
        return list;
    }

    public int getActualServerOnlineSave(String serverName){
        if(savingMethod == SaveType.YAML){
            return config.getConfig().getInt(ds.getSettings().getActualDate() + "." + serverName + ".maxOnline");

        }else{

        }

        return 0;
    }

    //YAML
    private int getMaxGlobalOnline(String actualDate){
        return config.getConfig().getInt(actualDate + ".maxTotalOnline");
    }

    private String getMaxGlobalTime(String actualDate){
        return config.getConfig().getString(actualDate + ".maxTotalOnlineTime");
    }

}
