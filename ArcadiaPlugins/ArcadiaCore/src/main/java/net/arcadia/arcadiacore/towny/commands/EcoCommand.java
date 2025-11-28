package net.arcadia.arcadiacore.towny.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.arcadia.arcadiacore.ArcadiaCore;
import net.arcadia.arcadiacore.data.ArcPlayerData;
import net.arcadia.arcadiacore.data.DataManager;
import net.arcadia.arcadiacore.towny.ArcadianTownyManager;
import net.arcadia.arcadiacore.towny.ArcadianTownyManager.CurrencyInfo;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class EcoCommand implements CommandExecutor, TabCompleter {

    private static final String CMD_KEY_ECO = "eco.main";

    private final ArcadianTownyManager manager;
    private final ArcadiaCore plugin;
    private final Economy econ;

    public EcoCommand(ArcadianTownyManager manager) {
        this.manager = manager;
        this.plugin = manager.getPlugin();
        this.econ = plugin.getEconomy();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // cmdperm via your DataManager
        if (sender instanceof Player player) {
            ArcPlayerData pd = manager.getPlugin()
                    .getDataManager()
                    .getOrCreate(player.getUniqueId(), player.getName());

            if (pd.hasExplicitRevoke(CMD_KEY_ECO)) {
                player.sendMessage(ChatColor.RED + "You are not allowed to use /" + label + ".");
                return true;
            }
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "currencylist" -> handleCurrencyList(sender);
            case "exchange" -> handleExchange(sender, label, args);
            default -> sendHelp(sender, label);
        }

        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GREEN + "=== /" + label + " ===");
        sender.sendMessage(ChatColor.AQUA + "/" + label + " currencylist "
                + ChatColor.AQUA + "- Show all approved currencies and their standards.");
        sender.sendMessage(ChatColor.AQUA + "/" + label + " exchange <amount> <from> <to> "
                + ChatColor.AQUA + "- Convert and MOVE money between currencies (₳ = standard).");
    }

    private void handleCurrencyList(CommandSender sender) {
        List<CurrencyInfo> list = manager.getAllCurrencies();
        sender.sendMessage(ChatColor.GREEN + "=== Approved Currencies ===");

        sender.sendMessage(ChatColor.AQUA + "- ₳ (Arcadian Standard, std = 1.0)");
        if (list.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No extra currencies have been approved yet.");
            return;
        }

        for (CurrencyInfo ci : list) {
            sender.sendMessage(ChatColor.AQUA + "- " + ci.name
                    + ChatColor.AQUA + " (standard: " + ci.standard + ")");
        }
    }

    private boolean isStandardKey(String key) {
        if (key == null) return false;
        key = key.toLowerCase(Locale.ROOT);
        return key.equals("standard")
                || key.equals("std")
                || key.equals("arc")
                || key.equals("arcadian")
                || key.equals("base")
                || key.equals("₳");
    }

    private void handleExchange(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " exchange <amount> <from> <to>");
            sender.sendMessage(ChatColor.YELLOW + "Example: /" + label + " exchange 100 std ArcadiaBucks");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
            return;
        }
        if (amount <= 0.0) {
            sender.sendMessage(ChatColor.RED + "Amount must be positive.");
            return;
        }

        String fromRaw = args[2];
        String toRaw = args[3];
        String from = fromRaw.toLowerCase(Locale.ROOT);
        String to = toRaw.toLowerCase(Locale.ROOT);

        boolean fromStd = isStandardKey(from);
        boolean toStd = isStandardKey(to);

        if (fromStd && toStd) {
            sender.sendMessage(ChatColor.RED + "You cannot exchange standard to standard.");
            return;
        }

        // Validate currencies & get standards
        double stdFrom = 1.0;
        double stdTo = 1.0;

        if (!fromStd) {
            if (!manager.isCurrencyApproved(fromRaw)) {
                sender.sendMessage(ChatColor.RED + "Unknown currency: " + fromRaw);
                return;
            }
            stdFrom = manager.getCurrencyStandard(fromRaw);
        }
        if (!toStd) {
            if (!manager.isCurrencyApproved(toRaw)) {
                sender.sendMessage(ChatColor.RED + "Unknown currency: " + toRaw);
                return;
            }
            stdTo = manager.getCurrencyStandard(toRaw);
        }

        // 1 <from> = stdFrom base units, 1 <to> = stdTo base units
        double rawResult = amount * stdFrom / stdTo;

        DataManager dm = plugin.getDataManager();
        ArcPlayerData pd = dm.getOrCreate(player.getUniqueId(), player.getName());

        // Actually move balances
        if (fromStd) {
            if (econ == null) {
                sender.sendMessage(ChatColor.RED + "Standard ₳ is disabled (no economy provider).");
                return;
            }

            double bal = econ.getBalance(player);
            if (bal < amount) {
                sender.sendMessage(ChatColor.RED + "Insufficient ₳. You have only "
                        + String.format(Locale.US, "%.2f", bal) + " ₳.");
                return;
            }

            EconomyResponse resp = econ.withdrawPlayer(player, amount);
            if (!resp.transactionSuccess()) {
                sender.sendMessage(ChatColor.RED + "Withdrawal failed: " + resp.errorMessage);
                return;
            }

            // Credit custom currency
            pd.addBalance(toRaw, rawResult);
            dm.saveAll();

            sender.sendMessage(ChatColor.GREEN + String.format(Locale.US,
                    "Exchanged %.2f ₳ into %.2f %s.",
                    amount, rawResult, toRaw));
            return;
        }

        if (toStd) {
            if (econ == null) {
                sender.sendMessage(ChatColor.RED + "Standard ₳ is disabled (no economy provider).");
                return;
            }

            double bal = pd.getBalance(fromRaw);
            if (bal < amount) {
                sender.sendMessage(ChatColor.RED + "Insufficient " + fromRaw + ". You have only "
                        + String.format(Locale.US, "%.2f", bal) + ".");
                return;
            }

            pd.addBalance(fromRaw, -amount);

            EconomyResponse resp = econ.depositPlayer(player, rawResult);
            if (!resp.transactionSuccess()) {
                // revert
                pd.addBalance(fromRaw, amount);
                sender.sendMessage(ChatColor.RED + "Deposit failed: " + resp.errorMessage);
                return;
            }

            dm.saveAll();

            sender.sendMessage(ChatColor.GREEN + String.format(Locale.US,
                    "Exchanged %.2f %s into %.2f ₳.",
                    amount, fromRaw, rawResult));
            return;
        }

        // wallet → wallet (no Vault)
        double bal = pd.getBalance(fromRaw);
        if (bal < amount) {
            sender.sendMessage(ChatColor.RED + "Insufficient " + fromRaw + ". You have only "
                    + String.format(Locale.US, "%.2f", bal) + ".");
            return;
        }

        pd.addBalance(fromRaw, -amount);
        pd.addBalance(toRaw, rawResult);
        dm.saveAll();

        sender.sendMessage(ChatColor.GREEN + String.format(Locale.US,
                "Exchanged %.2f %s into %.2f %s.",
                amount, fromRaw, rawResult, toRaw));
    }

    // ========= TAB COMPLETE =========

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {

        if (args.length == 1) {
            return prefixFilter(args[0], List.of("currencylist", "exchange"));
        }

        if (args[0].equalsIgnoreCase("exchange")) {
            if (args.length == 2) {
                return prefixFilter(args[1], List.of("1", "16", "32", "64", "100"));
            }
            if (args.length == 3 || args.length == 4) {
                List<String> names = new ArrayList<>();
                names.add("std");
                names.add("standard");
                names.add("₳");
                for (CurrencyInfo ci : manager.getAllCurrencies()) {
                    names.add(ci.name);
                }
                return prefixFilter(args[args.length - 1], names);
            }
        }

        return Collections.emptyList();
    }

    private List<String> prefixFilter(String token, List<String> options) {
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
