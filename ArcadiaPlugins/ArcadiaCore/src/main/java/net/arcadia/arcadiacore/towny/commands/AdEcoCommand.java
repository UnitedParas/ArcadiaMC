package net.arcadia.arcadiacore.towny.commands;

import net.arcadia.arcadiacore.ArcadiaCore;
import net.arcadia.arcadiacore.data.ArcPlayerData;
import net.arcadia.arcadiacore.data.DataManager;
import net.arcadia.arcadiacore.towny.ArcadianTownyManager;
import net.arcadia.arcadiacore.towny.ArcadianTownyManager.CurrencyRequest;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class AdEcoCommand implements CommandExecutor, TabCompleter {

    private final ArcadianTownyManager manager;
    private final SimpleDateFormat dateFmt =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT);

    public AdEcoCommand(ArcadianTownyManager manager) {
        this.manager = manager;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender,
                             Command command,
                             String label,
                             String[] args) {

        // Basic safety: require op or a permission.
        if (!sender.isOp() && !sender.hasPermission("arcadia.adeco")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use /" + label + ".");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String main = args[0].toLowerCase(Locale.ROOT);

        switch (main) {
            case "currency" -> handleCurrency(sender, label, args);
            case "forcedeposit" -> handleForcedDeposit(sender, label, args);
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown /" + label + " subcommand.");
                sendHelp(sender, label);
            }
        }

        return true;
    }

    // =========================================================
    // /adeco currency ...
    // =========================================================
    @SuppressWarnings("deprecation")
    private void handleCurrency(CommandSender sender, String label, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label
                    + " currency <waitlist|approve|reject> ...");
            return;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "waitlist" -> handleCurrencyWaitlist(sender);
            case "approve" -> handleCurrencyApprove(sender, label, args);
            case "reject" -> handleCurrencyReject(sender, label, args);
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown currency subcommand.");
                sender.sendMessage(ChatColor.RED + "Usage: /" + label
                        + " currency <waitlist|approve|reject> ...");
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void handleCurrencyWaitlist(CommandSender sender) {
        List<CurrencyRequest> list = manager.getCurrencyRequests();
        if (list == null || list.isEmpty()) {
            sender.sendMessage(ChatColor.AQUA + "There are currently "
                    + ChatColor.GREEN + "no"
                    + ChatColor.AQUA + " pending currency requests.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Pending Currency Requests ===");
        int i = 1;
        for (CurrencyRequest cr : list) {
            String idx = Integer.toString(i++);
            String timeStr = dateFmt.format(new Date(cr.time));

            sender.sendMessage(
                    ChatColor.GREEN + idx + ". " +
                    ChatColor.AQUA + cr.name +
                    ChatColor.AQUA + " (Nation: " + cr.nationName + ")" +
                    ChatColor.AQUA + " | Standard: " + cr.standard +
                    ChatColor.AQUA + " | By: " + cr.requesterUuid +
                    ChatColor.AQUA + " | " + timeStr
            );
        }
    }

    @SuppressWarnings("deprecation")
    private void handleCurrencyApprove(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " currency approve <currencyName>");
            sendPendingSuggestionLine(sender);
            return;
        }

        String name = args[2];

        boolean ok = manager.approveCurrency(name);
        if (!ok) {
            sender.sendMessage(ChatColor.RED + "No pending currency request found for: "
                    + ChatColor.AQUA + name);
            sendPendingSuggestionLine(sender);
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "Approved currency "
                + ChatColor.AQUA + name
                + ChatColor.GREEN + ".");
    }

    @SuppressWarnings("deprecation")
    private void handleCurrencyReject(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " currency reject <currencyName>");
            sendPendingSuggestionLine(sender);
            return;
        }

        String name = args[2];

        boolean ok = manager.rejectCurrency(name);
        if (!ok) {
            sender.sendMessage(ChatColor.RED + "No pending currency request found for: "
                    + ChatColor.AQUA + name);
            sendPendingSuggestionLine(sender);
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "Rejected currency "
                + ChatColor.AQUA + name
                + ChatColor.GREEN + ".");
    }

    @SuppressWarnings("deprecation")
    private void sendPendingSuggestionLine(CommandSender sender) {
        List<String> pending = manager.getQueuedCurrencyNames();
        if (pending == null || pending.isEmpty()) {
            sender.sendMessage(ChatColor.AQUA + "There are currently no pending currencies.");
            return;
        }

        sender.sendMessage(
                ChatColor.AQUA + "Pending currencies: "
                        + ChatColor.GREEN + String.join(ChatColor.AQUA + ", " + ChatColor.GREEN, pending)
        );
    }

    // =========================================================
    // /adeco forcedeposit <player> <amount> <currency>
    // =========================================================
    @SuppressWarnings("deprecation")
    private void handleForcedDeposit(CommandSender sender, String label, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " forcedeposit <player> <amount> <currency>");
            return;
        }

        String targetName = args[1];
        String amountStr = args[2];
        String currency = args[3];

        // Check currency actually exists (approved)
        Double std = manager.getCurrencyStandard(currency);
        if (std == null || std <= 0.0) {
            sender.sendMessage(ChatColor.RED + "Unknown currency: " + currency);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + amountStr);
            return;
        }

        if (amount <= 0.0) {
            sender.sendMessage(ChatColor.RED + "Amount must be positive.");
            return;
        }

        OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
        String realName = off.getName() != null ? off.getName() : targetName;

        ArcadiaCore core = manager.getPlugin();
        DataManager dm = core.getDataManager();
        ArcPlayerData pd = dm.getOrCreate(off.getUniqueId(), realName);

        pd.addBalance(currency, amount);
        dm.saveAll();

        sender.sendMessage(ChatColor.GREEN + "Deposited "
                + ChatColor.AQUA + String.format(Locale.US, "%.2f", amount) + " " + currency.toUpperCase(Locale.ROOT)
                + ChatColor.GREEN + " into " + ChatColor.AQUA + realName + ChatColor.GREEN + "'s wallet.");
    }

    // =========================================================
    // HELP
    // =========================================================
    @SuppressWarnings("deprecation")
    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "=== /" + label + " (Admin Economy) ===");
        sender.sendMessage(ChatColor.GREEN + "/" + label + " currency waitlist "
                + ChatColor.AQUA + "- View pending currency requests.");
        sender.sendMessage(ChatColor.GREEN + "/" + label + " currency approve <name> "
                + ChatColor.AQUA + "- Approve a pending currency.");
        sender.sendMessage(ChatColor.GREEN + "/" + label + " currency reject <name> "
                + ChatColor.AQUA + "- Reject a pending currency.");
        sender.sendMessage(ChatColor.GREEN + "/" + label + " forcedeposit <player> <amount> <currency> "
                + ChatColor.AQUA + "- Force-add currency to a player's wallet.");
    }

    // =========================================================
    // TAB COMPLETION
    // =========================================================
    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {

        if (!sender.isOp() && !sender.hasPermission("arcadia.adeco")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            // /adeco <currency|forcedeposit>
            return filterPrefix(args[0], List.of("currency", "forcedeposit"));
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("currency")) {
            if (args.length == 2) {
                // /adeco currency <waitlist|approve|reject>
                return filterPrefix(args[1], List.of("waitlist", "approve", "reject"));
            }
            if (args.length == 3 &&
                    (args[1].equalsIgnoreCase("approve") || args[1].equalsIgnoreCase("reject"))) {
                List<String> pending = manager.getQueuedCurrencyNames();
                if (pending == null) {
                    return Collections.emptyList();
                }
                return filterPrefix(args[2], pending);
            }
        }

        if (args[0].equalsIgnoreCase("forcedeposit")) {
            if (args.length == 2) {
                // player names – let Bukkit handle / return null
                return null;
            }
            if (args.length == 3) {
                // common amounts
                return filterPrefix(args[2], List.of("1", "10", "64", "100", "1000"));
            }
            if (args.length == 4) {
                // both queued + approved currencies
                List<String> names = new ArrayList<>();

                List<String> queued = manager.getQueuedCurrencyNames();
                if (queued != null) names.addAll(queued);

                manager.getAllCurrencies().forEach(ci -> {
                    if (!names.contains(ci.name)) names.add(ci.name);
                });

                return filterPrefix(args[3], names);
            }
        }

        return Collections.emptyList();
    }

    private List<String> filterPrefix(String token, List<String> options) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String s : options) {
            if (s.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(s);
            }
        }
        return out;
    }
}
