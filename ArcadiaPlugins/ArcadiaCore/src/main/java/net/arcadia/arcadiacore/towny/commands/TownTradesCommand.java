package net.arcadia.arcadiacore.towny.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import net.arcadia.arcadiacore.ArcadiaCore;
import net.arcadia.arcadiacore.data.RankManager;
import net.arcadia.arcadiacore.towny.trade.TownTradeManager;
import net.arcadia.arcadiacore.towny.trade.TownTradeManager.ExportListing;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class TownTradesCommand implements CommandExecutor {

    private final ArcadiaCore plugin;
    private final RankManager rankManager;
    private final TownTradeManager tradeManager;

    public TownTradesCommand(ArcadiaCore plugin, TownTradeManager tradeManager) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
        this.tradeManager = tradeManager;
    }

    private boolean hasMinRank(CommandSender sender, int min) {
        if (!(sender instanceof Player p)) return true;
        return rankManager.hasRankAtLeast(p.getUniqueId(), p.getName(), min);
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

        TownyAPI api = TownyAPI.getInstance();
        Resident res = api.getResident(player);
        if (res == null || !res.hasTown()) {
            player.sendMessage(ChatColor.RED + "You are not in a town.");
            return true;
        }

        Town ownTown = res.getTownOrNull();
        if (ownTown == null) {
            player.sendMessage(ChatColor.RED + "Could not resolve your town.");
            return true;
        }

        Town targetTown = ownTown;

        // /t trades (own town)
        // /t trades OtherTown (if same nation or allied nation)
        if (args.length >= 1) {
            String townName = args[0];

            Town candidate = api.getTown(townName);
            if (candidate == null) {
                player.sendMessage(ChatColor.RED + "Unknown town: " + townName);
                return true;
            }

            if (!candidate.equals(ownTown)) {
                Nation playerNation = res.getNationOrNull();
                Nation townNation = candidate.getNationOrNull();

                boolean allowed = false;
                if (playerNation != null && townNation != null) {
                    if (playerNation.equals(townNation)) {
                        allowed = true; // same nation
                    } else if (playerNation.hasAlly(townNation)) {
                        allowed = true; // allied nation
                    }
                }

                if (!allowed) {
                    player.sendMessage(ChatColor.RED + "You may only view trades from your own nation or allied nations.");
                    return true;
                }
                targetTown = candidate;
            }
        }

        List<ExportListing> exports = tradeManager.getExportsForTown(targetTown.getName());

        player.sendMessage(ChatColor.GOLD + "=== Trades for town: " + targetTown.getName() + " ===");
        if (exports.isEmpty()) {
            player.sendMessage(ChatColor.AQUA + "No exports listed.");
            return true;
        }

        for (ExportListing ex : exports) {
            String matName = ex.material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
            player.sendMessage(
                    ChatColor.AQUA + "- " + matName +
                    ChatColor.GRAY + " | Amount: " + ChatColor.WHITE + ex.amount +
                    ChatColor.GRAY + " | Batch: " + ChatColor.WHITE + ex.per +
                    ChatColor.GRAY + " | Price/batch: " + ChatColor.WHITE + ex.price
            );
        }

        return true;
    }
}
