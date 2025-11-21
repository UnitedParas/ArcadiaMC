package net.arcadia.arcadiacore.commands;

import net.arcadia.arcadiacore.ArcadiaCore;
import org.bukkit.command.*;

import java.util.List;

public class ModcallCommand implements CommandExecutor, TabCompleter {

    private final ArcadiaCore plugin;
    private final ArcCommand arcCommand;

    public ModcallCommand(ArcadiaCore plugin, ArcCommand arcCommand) {
        this.plugin = plugin;
        this.arcCommand = arcCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return arcCommand.handleModcall(sender, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return arcCommand.tabCompleteModcall(sender, args);
    }
}
