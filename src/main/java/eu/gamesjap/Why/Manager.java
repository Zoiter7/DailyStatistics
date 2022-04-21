package eu.gamesjap.Why;

import eu.gamesjap.Why.model.ServerData;
import net.md_5.bungee.api.config.ServerInfo;
import org.spicord.embed.Embed;
import org.spicord.embed.EmbedLoader;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Manager {

    private final DailyStatistics ds;

    public Manager(DailyStatistics ds){
        this.ds = ds;
    }

    public void collectInfo() {
        final List<String> serverList = ds.getConfigManager().getConfig().getStringList("servers");
        final String actualDate = ds.getSettings().getActualDate();

        final int totalPlayersOnline = ds.getProxy().getOnlineCount();
        ds.getLogger().info(ds.prefix + "§aCollecting info about total online players right now §7(§c" + totalPlayersOnline + "§7)");

        Map<String, Integer> servers = new HashMap<>();

        for (int i = 0; i < serverList.size(); i++) {
            ServerInfo sv = ds.getProxy().getServerInfo(serverList.get(i));
            if (sv == null) continue;

            int online = sv.getPlayers().size();

            if (online != 0) {
                int actualSave = ds.getDataManager().getActualServerOnlineSave(serverList.get(i));

                if (online > actualSave) servers.put(serverList.get(i), online);
            }
        }

        ds.getDataManager().saveData(servers, totalPlayersOnline, actualDate);

    }

    public void prepareDiscordMessage(String date, boolean command, String cmdAction) {
        if(date == null) date = ds.getSettings().getActualDate();

        final List<ServerData> serverData = ds.getDataManager().getData(date);

        if (command) {
            if (serverData.isEmpty()) {
                String msg = "";
                if (cmdAction.equals("stat")) {
                    msg = ds.getConfigManager().getConfig().getString("dMessage-noStatFound");
                } else if (cmdAction.equals("actual")) {
                    msg = ds.getConfigManager().getConfig().getString("dMessage-noStatReady");
                }
                ds.addon.executeMsg(null, msg);
                return;
            }
        }else{
            ds.getLogger().info("Sending daily stats message to Discord!");
        }

        File folder = new File(ds.getDataFolder(), "messages");
        File file = new File(folder, "statMessage.json");

        if (!file.exists()) {
            ds.getLogger().info(ds.prefix + "§cERROR: The message file (statMessage.json) was not found.");
            return;
        }

        EmbedLoader loader = new EmbedLoader();
        loader.load(folder);

        Embed firstLoad = loader.getEmbedByName("statMessage");

        String replace = firstLoad.toString();

        for(ServerData server : serverData){
            if(server.getServerName().equalsIgnoreCase("7global7")) continue;

            //replace.replace("%" + server.getServerName() + "%", server.getServerName());
            replace = replace.replace("%" + server.getServerName() + "_online%", server.getMaxOnline());
            replace = replace.replace("%" + server.getServerName() + "_time%", server.getTime());
        }

        ServerData globalServer = serverData.stream().filter(server -> server.getServerName().equalsIgnoreCase("7global7")).findFirst().get();

        replace = replace.replace("%global_online%", globalServer.getMaxOnline());
        replace = replace.replace("%global_time%", globalServer.getTime());
        replace = replace.replace("%date%", date);

        Embed edited = Embed.fromJson(replace);
        ds.addon.executeMsg(edited, null);

    }
}
