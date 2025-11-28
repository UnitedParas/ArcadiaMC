package net.arcadia.arcadiacore.commands;

import net.arcadia.arcadiacore.ArcadiaCore;
import net.arcadia.arcadiacore.data.RankManager;
import net.arcadia.arcadiacore.towny.trade.TownTradeManager;
import net.arcadia.arcadiacore.towny.trade.TownTradeManager.ExportListing;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Simple global export market view.
 * Registered to whatever command you wired it to in ArcadiaCore (currently /eco before Towny setup).
 *
 * It lists all exports from all towns and sorts them by cheapest unit price (price / per).
 */
public class EcoMarketCommand implements CommandExecutor {

    private final ArcadiaCore plugin;
    private final RankManager rankManager;
    private final TownTradeManager tradeManager;

    public EcoMarketCommand(ArcadiaCore plugin, TownTradeManager tradeManager) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
        this.tradeManager = tradeManager;
    }

    private boolean hasMinRank(CommandSender sender, int min) {
        if (!(sender instanceof Player p)) return true; // console allowed
        return rankManager.hasRankAtLeast(p.getUniqueId(), p.getName(), min);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender,
                             Command command,
                             String label,
                             String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        if (!hasMinRank(player, 1)) {
            player.sendMessage(ChatColor.RED + "Insufficient rank for this command.");
            return true;
        }

        List<ExportListing> all = tradeManager.getAllExports();
        if (all.isEmpty()) {
            player.sendMessage(ChatColor.AQUA + "There are currently no export market listings.");
            return true;
        }

        // Sort by cheapest unit price (price per single item)
        all.sort(Comparator.comparingDouble(ex -> ex.price / ex.per));

        player.sendMessage(ChatColor.GOLD + "=== Arcadia Export Market ===");
        for (ExportListing ex : all) {
            String matName = ex.material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
            String nationPart = (ex.nationName != null && !ex.nationName.isEmpty())
                    ? ChatColor.GRAY + " | Nation: " + ChatColor.WHITE + ex.nationName
                    : "";
            double unitPrice = ex.price / ex.per;

            player.sendMessage(
                    ChatColor.AQUA + "- " + matName +
                    ChatColor.GRAY + " | Town: " + ChatColor.WHITE + ex.townName +
                    ChatColor.GRAY + " | Amount: " + ChatColor.WHITE + ex.amount +
                    ChatColor.GRAY + " | Batch: " + ChatColor.WHITE + ex.per +
                    ChatColor.GRAY + " | Price/batch: " + ChatColor.WHITE + ex.price +
                    ChatColor.GRAY + " | ≈/item: " + ChatColor.WHITE +
                    String.format(Locale.US, "%.2f", unitPrice) +
                    nationPart
            );
        }

        return true;
    }
}
