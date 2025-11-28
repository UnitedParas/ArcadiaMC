package net.arcadia.arcadiacore.towny.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import net.arcadia.arcadiacore.ArcadiaCore;
import net.arcadia.arcadiacore.data.RankManager;
import net.arcadia.arcadiacore.towny.trade.TownTradeManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TownUnlistExportCommand implements CommandExecutor {

    private final ArcadiaCore plugin;
    private final RankManager rankManager;
    private final TownTradeManager tradeManager;

    public TownUnlistExportCommand(ArcadiaCore plugin, TownTradeManager tradeManager) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
        this.tradeManager = tradeManager;
    }

    private boolean hasMinRank(CommandSender sender, int min) {
        if (!(sender instanceof Player p)) return true;
        return rankManager.hasRankAtLeast(p.getUniqueId(), p.getName(), min);
    }

    private boolean isMayorOrCoMayor(Resident res) {
        if (res == null) return false;
        if (res.isMayor()) return true;
        return res.hasTownRank("assistant");
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        if (!hasMinRank(player, 1)) {
            player.sendMessage(ChatColor.RED + "Insufficient rank for this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.GREEN + "/t unlistexport <item>");
            sender.sendMessage(ChatColor.AQUA + "Remove all export listings for a given item from your town.");
            return true;
        }

        String itemName = args[0];
        Material mat = Material.matchMaterial(itemName);
        if (mat == null) {
            sender.sendMessage(ChatColor.RED + "Unknown item: " + itemName);
            return true;
        }

        TownyAPI api = TownyAPI.getInstance();
        Resident res = api.getResident(player);
        if (res == null || !res.hasTown()) {
            sender.sendMessage(ChatColor.RED + "You are not in a town.");
            return true;
        }

        if (!isMayorOrCoMayor(res)) {
            sender.sendMessage(ChatColor.RED + "Only the mayor or co-mayor (assistant) can unlist exports.");
            return true;
        }

        Town town = res.getTownOrNull();
        if (town == null) {
            sender.sendMessage(ChatColor.RED + "Could not resolve your town.");
            return true;
        }

        int removed = tradeManager.removeExportsByMaterial(town.getName(), mat);
        if (removed == 0) {
            sender.sendMessage(ChatColor.YELLOW + "No exports found for that item.");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Removed " + removed + " export listing(s) for that item.");
        }

        return true;
    }
}
