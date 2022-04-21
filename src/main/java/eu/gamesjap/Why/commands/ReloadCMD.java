package eu.gamesjap.Why.commands;

import eu.gamesjap.Why.DailyStatistics;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class ReloadCMD extends Command {

    private final DailyStatistics ds;

    public ReloadCMD(DailyStatistics ds) {
        super("ds");
        this.ds = ds;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(!sender.hasPermission("ds.reload")) return;

        if (args.length == 0) {
            sender.sendMessage(new TextComponent(ds.prefix + "§cUsage: /ds reload"));
        } else if (args[0].equalsIgnoreCase("reload")) {
            ds.reload();
            sender.sendMessage(new TextComponent(ds.prefix + "§aThe configuration has been reloaded!"));
        }
    }
}
