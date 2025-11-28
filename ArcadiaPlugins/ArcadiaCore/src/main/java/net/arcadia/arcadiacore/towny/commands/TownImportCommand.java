package net.arcadia.arcadiacore.towny.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import net.arcadia.arcadiacore.ArcadiaCore;
import net.arcadia.arcadiacore.data.ArcPlayerData;
import net.arcadia.arcadiacore.data.RankManager;
import net.arcadia.arcadiacore.towny.trade.TownTradeManager;
import net.arcadia.arcadiacore.towny.trade.TownTradeManager.ExportListing;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TownImportCommand implements CommandExecutor {

    private static final String CMD_KEY_IMPORT = "town.trades.import";

    private final ArcadiaCore plugin;
    private final RankManager rankManager;
    private final TownTradeManager tradeManager;

    public TownImportCommand(ArcadiaCore plugin,
                             RankManager rankManager,
                             TownTradeManager tradeManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        this.tradeManager = tradeManager;
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

        if (!hasMinimumRank(player, 1)) {
            player.sendMessage(ChatColor.RED + "You are not high enough rank to use town trades.");
            return true;
        }

        if (isCmdKeyRevoked(player, CMD_KEY_IMPORT)) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use /t import.");
            return true;
        }

        if (args.length < 3) {
            sendUsage(player, label);
            return true;
        }

        Town myTown = getTown(player);
        if (myTown == null) {
            return true;
        }

        if (!isMayorOrCo(player, myTown)) {
            player.sendMessage(ChatColor.RED + "Only the mayor or a co-mayor can import for the town.");
            return true;
        }

        String itemToken = args[0];
        String amountToken = args[1];
        String exporterTownName = args[2];

        Material mat = Material.matchMaterial(itemToken);
        if (mat == null) {
            player.sendMessage(ChatColor.RED + "Unknown item: " + itemToken);
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountToken);
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Invalid amount: " + amountToken);
            return true;
        }
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Amount must be positive.");
            return true;
        }

        // Verify exporting town exists
        Town exporterTown = getTownByName(exporterTownName);
        if (exporterTown == null) {
            player.sendMessage(ChatColor.RED + "Unknown exporting town: " + exporterTownName);
            return true;
        }

        // Find an export listing for that town + material
        List<ExportListing> exports = tradeManager.getExportsForTown(exporterTown.getName());
        ExportListing listing = null;
        for (ExportListing ex : exports) {
            if (ex.material == mat) {
                listing = ex;
                break;
            }
        }

        if (listing == null) {
            player.sendMessage(ChatColor.RED + "That town does not export that item.");
            return true;
        }

        if (listing.amount < amount) {
            player.sendMessage(ChatColor.RED + "That town does not have enough of that item listed.");
            return true;
        }

        boolean ok = tradeManager.decreaseExportAmount(exporterTown.getName(), mat, amount);
        if (!ok) {
            player.sendMessage(ChatColor.RED + "Failed to complete import; please try again.");
            return true;
        }

        String matName = mat.name().toLowerCase(Locale.ROOT).replace('_', ' ');

        player.sendMessage(ChatColor.GREEN + "Imported " + amount + " " + matName + " from " + exporterTown.getName() + ".");
        player.sendMessage(ChatColor.AQUA + "Items are considered delivered to your TradePort barrels. (Physical movement TODO.)");

        return true;
    }

    @SuppressWarnings("deprecation")
    private void sendUsage(Player player, String label) {
        player.sendMessage(ChatColor.GREEN + "/t import <item> <amount> <exportingTown>");
        player.sendMessage(ChatColor.AQUA + "Buy items from another town's exports.");
        player.sendMessage(ChatColor.AQUA + "Example: " + ChatColor.GREEN + "/t import diamond 32 Haven");
    }

    @SuppressWarnings("deprecation")
    private Town getTown(Player player) {
        TownyAPI api = TownyAPI.getInstance();
        Resident res = api.getResident(player);
        if (res == null || !res.hasTown()) {
            player.sendMessage(ChatColor.RED + "You are not in a town.");
            return null;
        }
        return res.getTownOrNull();
    }

    private Town getTownByName(String name) {
        TownyAPI api = TownyAPI.getInstance();
        try {
            return api.getTown(name);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isMayorOrCo(Player player, Town town) {
        TownyAPI api = TownyAPI.getInstance();
        Resident res = api.getResident(player);
        if (res == null) return false;

        try {
            if (town.isMayor(res)) return true;
        } catch (Exception ignored) {
        }

        return res.hasTownRank("co-mayor");
    }

    private boolean hasMinimumRank(Player player, int minRankValue) {
        return rankManager.hasRankAtLeast(
                player.getUniqueId(),
                player.getName(),
                minRankValue
        );
    }

    private boolean isCmdKeyRevoked(Player player, String cmdKey) {
        var dataManager = plugin.getDataManager();
        ArcPlayerData pd = dataManager.getOrCreate(player.getUniqueId(), player.getName());
        Set<String> revokes = pd.getRevokes();
        return revokes.contains(cmdKey);
    }
}
