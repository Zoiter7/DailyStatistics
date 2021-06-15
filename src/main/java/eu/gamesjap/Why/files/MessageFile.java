package eu.gamesjap.Why.files;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import eu.gamesjap.Why.DailyStatistics;

public class MessageFile {

    private static File messageFile;

    public void createFile() {

        String folderPath = (DailyStatistics.plugin.getDataFolder() + File.separator + "messages");
        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdir();
        }

        messageFile = new File(folder + File.separator + "statMessage" + ".json");

        if (!messageFile.exists()) {
            try {
                InputStream in = getClass().getClassLoader().getResourceAsStream("statMessage.json");
                Files.copy(in, messageFile.toPath());
            } catch (IOException e) {
                DailyStatistics.plugin.getProxy().getConsole()
                        .sendMessage("§cAn error has occurred when creating file...");
            }
        }

    }
}
