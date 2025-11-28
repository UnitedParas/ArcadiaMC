package net.arcadia.arcadiacore.towny.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.arcadia.arcadiacore.towny.ArcadianTownyManager;
import net.arcadia.arcadiacore.towny.ArcadianTownyManager.MarriageProposal;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class NationPolMarriageCommand implements CommandExecutor {

    private final ArcadianTownyManager manager;

    public NationPolMarriageCommand(ArcadianTownyManager manager) {
        this.manager = manager;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        if (args.length < 1) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "propose" -> handlePropose(player, args);
            case "accept" -> handleAccept(player);
            case "reject" -> handleReject(player);
            default -> sendHelp(player);
        }

        return true;
    }

    @SuppressWarnings("deprecation")
    private void handlePropose(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.AQUA + "Usage: /n polmarriage propose <player>");
            return;
        }

        String targetName = args[1];
        Player target = player.getServer().getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found or not online.");
            return;
        }

        UUID proposerId = player.getUniqueId();
        UUID targetId = target.getUniqueId();

        if (proposerId.equals(targetId)) {
            player.sendMessage(ChatColor.RED + "You cannot propose to yourself...");
            return;
        }

        // Simple one-pending-per-target rule
        boolean created = manager.createMarriageProposal(proposerId, targetId);
        if (!created) {
            player.sendMessage(ChatColor.RED + "That player already has a pending political marriage proposal.");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "You proposed a political marriage to " + target.getName() + ".");
        target.sendMessage(ChatColor.GREEN + player.getName() + " has proposed a political marriage to you.");
        target.sendMessage(ChatColor.AQUA + "Use /n polmarriage accept or /n polmarriage reject.");
    }

    @SuppressWarnings("deprecation")
    private void handleAccept(Player player) {
        UUID targetId = player.getUniqueId();
        MarriageProposal proposal = manager.getProposalForTarget(targetId);
        if (proposal == null) {
            player.sendMessage(ChatColor.RED + "You have no pending political marriage proposals...");
            return;
        }

        Player proposer = player.getServer().getPlayer(proposal.proposer);
        String proposerName = proposer != null ? proposer.getName() : "Unknown";

        manager.setEngaged(proposal.proposer, targetId);
        manager.removeProposal(targetId);

        player.sendMessage(ChatColor.GREEN + "You are now engaged to " + proposerName + "!");
        if (proposer != null) {
            proposer.sendMessage(ChatColor.GREEN + player.getName() + " has accepted your marriage proposal. You are now engaged!");
        }
    }

    @SuppressWarnings("deprecation")
    private void handleReject(Player player) {
        UUID targetId = player.getUniqueId();
        MarriageProposal proposal = manager.getProposalForTarget(targetId);
        if (proposal == null) {
            player.sendMessage(ChatColor.RED + "You have no pending political marriage proposals.");
            return;
        }

        Player proposer = player.getServer().getPlayer(proposal.proposer);
        String proposerName = proposer != null ? proposer.getName() : "Unknown";

        manager.removeProposal(targetId);
        player.sendMessage(ChatColor.YELLOW + "You rejected the political marriage proposal from " + proposerName + ".");
        if (proposer != null) {
            proposer.sendMessage(ChatColor.RED + player.getName() + " has rejected your marriage proposal.");
        }
    }

    @SuppressWarnings("deprecation")
    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GREEN + "=== /N PolMarriage ===");
        player.sendMessage(ChatColor.AQUA + "/n polmarriage propose <player> " + ChatColor.GRAY + "- Propose a political marriage.");
        player.sendMessage(ChatColor.AQUA + "/n polmarriage accept " + ChatColor.GRAY + "- Accept a proposal.");
        player.sendMessage(ChatColor.AQUA + "/n polmarriage reject " + ChatColor.GRAY + "- Reject a proposal.");
    }
}
