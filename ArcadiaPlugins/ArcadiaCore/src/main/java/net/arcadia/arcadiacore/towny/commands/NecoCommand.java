package net.arcadia.arcadiacore.towny.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.arcadia.arcadiacore.ArcadiaCore;
import net.arcadia.arcadiacore.data.ArcPlayerData;
import net.arcadia.arcadiacore.towny.ArcadianTownyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

public class NecoCommand implements CommandExecutor {

    // cmdperm key for this command, so /arcad cmdperm grant <player> eco.neco works.
    private static final String CMD_KEY_NECO = "eco.neco";

    private final ArcadianTownyManager manager;

    public NecoCommand(ArcadianTownyManager manager) {
        this.manager = manager;
    }

    // ---- Core entrypoint ----

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        // cmdperm check (eco.neco)
        if (!canUseCmdKey(player, CMD_KEY_NECO)) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use /" + label + ".");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "setcurrency" -> handleSetCurrency(player, label, args);
            case "reqcurrency" -> handleReqCurrency(player, label, args);
            case "exchangetax" -> handleExchangeTax(player, label, args);
            default -> sendHelp(player, label);
        }

        return true;
    }

    // ---- Helpers: plugin / data / permissions ----

    private ArcadiaCore getCore() {
        return (ArcadiaCore) Bukkit.getPluginManager().getPlugin("ArcadiaCore");
    }

    private boolean canUseCmdKey(Player player, String cmdKey) {
        ArcadiaCore core = getCore();
        if (core == null) {
            // If something is misconfigured, fail open for OPs, otherwise fail closed.
            return player.isOp();
        }

        var dataManager = core.getDataManager();
        ArcPlayerData pd = dataManager.getOrCreate(player.getUniqueId(), player.getName());

        // Hard revoke beats everything.
        if (pd.isCommandRevoked(cmdKey)) {
            return false;
        }

        // If explicitly granted, allow.
        if (pd.isCommandGranted(cmdKey)) {
            return true;
        }

        // Fallback: allow ops by default, you can tighten this later if you want.
        return player.isOp();
    }

    @SuppressWarnings("deprecation")
    private Nation getNation(Player player) {
        TownyAPI api = TownyAPI.getInstance();
        Resident res = api.getResident(player);
        if (res == null || !res.hasNation()) {
            player.sendMessage(ChatColor.RED + "You are not in a nation.");
            return null;
        }
        return res.getNationOrNull();
    }

    private boolean isLeader(Player player, Nation nation) {
        Resident res = TownyAPI.getInstance().getResident(player);
        return res != null && res.isKing();
    }

    // ---- /neco setcurrency <currency> ----

    @SuppressWarnings("deprecation")
    private void handleSetCurrency(Player player, String label, String[] args) {
        Nation nation = getNation(player);
        if (nation == null) return;

        if (!isLeader(player, nation)) {
            player.sendMessage(ChatColor.RED + "Only the nation leader can set the national currency.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " setcurrency <currency>");
            return;
        }

        String currency = args[1];

        boolean exists = manager.getAllCurrencies().stream()
                .anyMatch(c -> c.name.equalsIgnoreCase(currency));
        if (!exists) {
            player.sendMessage(ChatColor.RED + "That currency is not approved.");
            return;
        }

        manager.setNationCurrency(nation.getName(), currency);
        player.sendMessage(ChatColor.GREEN + "Set national currency of " + nation.getName()
                + " to " + ChatColor.AQUA + currency + ChatColor.GREEN + ".");
    }

    // ---- /neco reqcurrency <name> <standard> ----

    @SuppressWarnings("deprecation")
    private void handleReqCurrency(Player player, String label, String[] args) {
        Nation nation = getNation(player);
        if (nation == null) return;

        if (args.length < 3) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " reqcurrency <name> <standard>");
            player.sendMessage(ChatColor.YELLOW + "Example: /" + label + " reqcurrency ArcadianCoin 1.0");
            return;
        }

        String name = args[1];
        String standardStr = args[2];

        double standard;
        try {
            standard = Double.parseDouble(standardStr);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid standard value: " + standardStr);
            return;
        }

        if (standard <= 0.0) {
            player.sendMessage(ChatColor.RED + "Standard must be a positive number.");
            return;
        }

        UUID uuid = player.getUniqueId();
        boolean ok = manager.enqueueCurrencyRequest(uuid.toString(), nation.getName(), name, standard);
        if (!ok) {
            player.sendMessage(ChatColor.RED + "That currency already has a pending request.");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "Requested new currency "
                + ChatColor.AQUA + "\"" + name + "\"" + ChatColor.GREEN
                + " with standard " + ChatColor.AQUA + standard + ChatColor.GREEN + " for approval.");
    }

    // ---- /neco exchangetax <currency> <percent> ----
    // Stores: eco.taxes.<currencyLower> = percent

    @SuppressWarnings("deprecation")
    private void handleExchangeTax(Player player, String label, String[] args) {
        Nation nation = getNation(player);
        if (nation == null) return;

        if (!isLeader(player, nation)) {
            player.sendMessage(ChatColor.RED + "Only the nation leader can set exchange tax.");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " exchangetax <currency> <1-100>");
            return;
        }

        String currency = args[1];
        String percentStr = args[2];

        boolean exists = manager.getAllCurrencies().stream()
                .anyMatch(c -> c.name.equalsIgnoreCase(currency));
        if (!exists) {
            player.sendMessage(ChatColor.RED + "That currency is not approved.");
            return;
        }

        double percent;
        try {
            percent = Double.parseDouble(percentStr);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid percentage: " + percentStr);
            return;
        }

        if (percent < 0.0 || percent > 100.0) {
            player.sendMessage(ChatColor.RED + "Tax must be between 0 and 100.");
            return;
        }

        ArcadiaCore core = getCore();
        if (core == null) {
            player.sendMessage(ChatColor.RED + "Core plugin not found.");
            return;
        }

        String key = currency.toLowerCase(Locale.ROOT);
        core.getConfig().set("eco.taxes." + key, percent);
        core.saveConfig();

        player.sendMessage(ChatColor.GREEN + "Set exchange tax for "
                + ChatColor.AQUA + currency
                + ChatColor.GREEN + " to "
                + ChatColor.AQUA + String.format(Locale.US, "%.1f", percent) + "%");
    }

    // ---- Help text ----

    @SuppressWarnings("deprecation")
    private void sendHelp(Player player, String label) {
        player.sendMessage(ChatColor.GREEN + "=== /" + label + " (Nation Economy) ===");
        player.sendMessage(ChatColor.GREEN + "/" + label + " setcurrency <currency> "
                + ChatColor.AQUA + "- Set your nation's approved currency.");
        player.sendMessage(ChatColor.GREEN + "/" + label + " reqcurrency <name> <standard> "
                + ChatColor.AQUA + "- Request a new currency and its standard value.");
        player.sendMessage(ChatColor.GREEN + "/" + label + " exchangetax <currency> <1-100> "
                + ChatColor.AQUA + "- Set tax when exchanging into this currency.");
    }
}
