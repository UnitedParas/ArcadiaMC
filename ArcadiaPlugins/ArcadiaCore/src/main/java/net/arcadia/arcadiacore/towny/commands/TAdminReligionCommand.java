package net.arcadia.arcadiacore.towny.commands;

import net.arcadia.arcadiacore.towny.ArcadianTownyManager;
import net.arcadia.arcadiacore.towny.ArcadianTownyManager.ReligionRequest;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class TAdminReligionCommand implements CommandExecutor {

    private final ArcadianTownyManager manager;

    public TAdminReligionCommand(ArcadianTownyManager manager) {
        this.manager = manager;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("arcadia.tadmin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "religionwaitlist" -> {
                handleWaitlist(sender);
                return true;
            }
            case "approvereligion" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /tadmin approvereligion <name>");
                    return true;
                }
                manager.approveReligion(args[1]);
                sender.sendMessage(ChatColor.GREEN + "Approved religion: " + args[1]);
                return true;
            }
            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void handleWaitlist(CommandSender sender) {
        List<ReligionRequest> list = manager.getReligionRequests();
        sender.sendMessage(ChatColor.GOLD + "=== Religion Waitlist ===");
        if (list.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No pending religion requests.");
            return;
        }
        for (ReligionRequest rr : list) {
            sender.sendMessage(ChatColor.AQUA + rr.religionName +
                    ChatColor.GRAY + " requested by " + rr.getRequesterName() +
                    " (nation: " + rr.nationName + ")");
        }
    }

    @SuppressWarnings("deprecation")
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== /tadmin ===");
        sender.sendMessage(ChatColor.YELLOW + "/tadmin religionwaitlist");
        sender.sendMessage(ChatColor.YELLOW + "/tadmin approvereligion <name>");
    }
}
