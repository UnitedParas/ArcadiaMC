package net.arcadia.arcadiacore.towny.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.arcadia.arcadiacore.towny.ArcadianTownyManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PriestCommand implements CommandExecutor {

    private final ArcadianTownyManager manager;

    public PriestCommand(ArcadianTownyManager manager) {
        this.manager = manager;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player priest)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        if (args.length < 3 || !"marry".equalsIgnoreCase(args[0])) {
            priest.sendMessage(ChatColor.YELLOW + "Usage: /priest marry <player1> <player2>");
            return true;
        }

        String p1Name = args[1];
        String p2Name = args[2];

        Player p1 = priest.getServer().getPlayerExact(p1Name);
        Player p2 = priest.getServer().getPlayerExact(p2Name);

        if (p1 == null || p2 == null) {
            priest.sendMessage(ChatColor.RED + "Both players must be online.");
            return true;
        }

        // Check priest is actually a nation priest
        TownyAPI api = TownyAPI.getInstance();
        Resident priestRes = api.getResident(priest);
        if (priestRes == null || !priestRes.hasNation()) {
            priest.sendMessage(ChatColor.RED + "You are not in a nation.");
            return true;
        }
        Nation nation = priestRes.getNationOrNull();
        if (nation == null) {
            priest.sendMessage(ChatColor.RED + "Nation not found.");
            return true;
        }

        if (!manager.isNationPriest(nation.getName(), priest.getUniqueId())) {
            priest.sendMessage(ChatColor.RED + "You are not the nation priest.");
            return true;
        }

        UUID p1Id = p1.getUniqueId();
        UUID p2Id = p2.getUniqueId();

        UUID e1 = manager.getEngagedTo(p1Id);
        UUID e2 = manager.getEngagedTo(p2Id);

        if (e1 == null || !e1.equals(p2Id) || e2 == null || !e2.equals(p1Id)) {
            priest.sendMessage(ChatColor.RED + "These two players are not engaged to each other.");
            return true;
        }

        // Perform marriage
        manager.clearEngagement(p1Id, p2Id);
        manager.setMarried(p1Id, p2Id);

        p1.sendMessage(ChatColor.LIGHT_PURPLE + "You have been married to " + p2.getName() + " by " + priest.getName() + "!");
        p2.sendMessage(ChatColor.LIGHT_PURPLE + "You have been married to " + p1.getName() + " by " + priest.getName() + "!");
        priest.sendMessage(ChatColor.GREEN + "You have married " + p1.getName() + " and " + p2.getName() + ".");

        return true;
    }
}
