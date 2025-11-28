package net.arcadia.arcadiacore.towny.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import net.arcadia.arcadiacore.ArcadiaCore;
import net.arcadia.arcadiacore.data.ArcPlayerData;
import net.arcadia.arcadiacore.data.RankManager;
import net.arcadia.arcadiacore.towny.trade.TownTradeManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Set;

public class TownExportCommand implements CommandExecutor {

    // Command key for /t export, controlled via /arcad cmdperm
    private static final String CMD_KEY_EXPORT = "town.trades.export";

    private final ArcadiaCore plugin;
    private final RankManager rankManager;
    private final TownTradeManager tradeManager;

    public TownExportCommand(ArcadiaCore plugin,
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

        // Basic rank floor: rank value >= 1 (as you requested for trade stuff)
        if (!hasMinimumRank(player, 1)) {
            player.sendMessage(ChatColor.RED + "You are not high enough rank to use town trades.");
            return true;
        }

        // Command-key based override: if explicitly revoked, block.
        if (isCmdKeyRevoked(player, CMD_KEY_EXPORT)) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use /t export.");
            return true;
        }

        if (args.length < 4) {
            sendUsage(player, label);
            return true;
        }

        Town town = getTown(player);
        if (town == null) {
            return true; // message already sent
        }

        if (!isMayorOrCo(player, town)) {
            player.sendMessage(ChatColor.RED + "Only the mayor or a co-mayor can set town exports.");
            return true;
        }

        String itemToken = args[0];
        String amountToken = args[1];
        String priceToken = args[2];
        String perToken = args[3];

        Material mat = Material.matchMaterial(itemToken);
        if (mat == null) {
            player.sendMessage(ChatColor.RED + "Unknown item: " + itemToken);
            return true;
        }

        int amount;
        int per;
        double price;

        try {
            amount = Integer.parseInt(amountToken);
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Invalid amount: " + amountToken);
            return true;
        }

        try {
            price = Double.parseDouble(priceToken);
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Invalid price: " + priceToken);
            return true;
        }

        try {
            per = Integer.parseInt(perToken);
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Invalid per-value: " + perToken);
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Amount must be positive.");
            return true;
        }
        if (per <= 0 || per > 64) {
            player.sendMessage(ChatColor.RED + "Per must be between 1 and 64.");
            return true;
        }
        if (price <= 0.0) {
            player.sendMessage(ChatColor.RED + "Price must be positive.");
            return true;
        }

        // We store nationName for display in market listing.
        String nationName = null;
        try {
            if (town.hasNation()) {
                Nation nation = town.getNationOrNull();
                if (nation != null) {
                    nationName = nation.getName();
                }
            }
        } catch (Exception ignored) {
        }

        boolean ok = tradeManager.addOrUpdateExport(
                town.getName(),
                nationName,
                mat,
                amount,
                price,
                per
        );

        if (!ok) {
            player.sendMessage(ChatColor.RED + "Failed to create export listing. Check your values and try again.");
            return true;
        }

        String matName = mat.name().toLowerCase(Locale.ROOT).replace('_', ' ');

        player.sendMessage(ChatColor.GREEN + "Created/updated export listing for your town.");
        player.sendMessage(ChatColor.AQUA + "Item: " + ChatColor.GREEN + matName);
        player.sendMessage(ChatColor.AQUA + "Amount: " + ChatColor.GREEN + amount);
        player.sendMessage(ChatColor.AQUA + "Price: " + ChatColor.GREEN + price + ChatColor.AQUA + " per " + ChatColor.GREEN + per);
        if (nationName != null) {
            player.sendMessage(ChatColor.AQUA + "Nation: " + ChatColor.GREEN + nationName);
        }

        return true;
    }

    @SuppressWarnings("deprecation")
    private void sendUsage(Player player, String label) {
        player.sendMessage(ChatColor.GREEN + "/t export <item> <amount> <price> <per>" );
        player.sendMessage(ChatColor.AQUA + "Put items up on the town market.");
        player.sendMessage(ChatColor.AQUA + "Example: " + ChatColor.GREEN + "/t export diamond 64 5.0 16");
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

    private boolean isMayorOrCo(Player player, Town town) {
        TownyAPI api = TownyAPI.getInstance();
        Resident res = api.getResident(player);
        if (res == null) return false;

        try {
            if (town.isMayor(res)) return true;
        } catch (Exception ignored) {
        }

        // You can change "co-mayor" to whatever rank label you actually use.
        return res.hasTownRank("co-mayor");
    }

    private boolean hasMinimumRank(Player player, int minRankValue) {
        // Uses your RankManager numeric values
        return rankManager.hasRankAtLeast(
                player.getUniqueId(),
                player.getName(),
                minRankValue
        );
    }

    /**
     * Checks ArcPlayerData revokes/grants for a specific command key.
     * We only *block* if revoked; otherwise allowed.
     */
    private boolean isCmdKeyRevoked(Player player, String cmdKey) {
        var dataManager = plugin.getDataManager();
        ArcPlayerData pd = dataManager.getOrCreate(player.getUniqueId(), player.getName());

        Set<String> revokes = pd.getRevokes();
        return revokes.contains(cmdKey);
    }
}
