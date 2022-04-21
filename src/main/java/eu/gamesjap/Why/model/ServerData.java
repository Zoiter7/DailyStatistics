package eu.gamesjap.Why.model;

public class ServerData {

    private String serverName;
    private int maxOnline;
    private String time;

    public ServerData(String name, int online, String date){
        this.serverName = name;
        this.maxOnline = online;
        this.time = date;
    }

    public String getServerName() {
        return serverName;
    }

    public String getMaxOnline() {
        return String.valueOf(maxOnline);
    }

    public String getTime() {
        return time;
    }
}
