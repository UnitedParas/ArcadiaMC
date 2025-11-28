package net.arcadia.arcadiacore.commands;

import net.arcadia.arcadiacore.ArcadiaCore;
import net.arcadia.arcadiacore.data.ArcPlayerData;
import net.arcadia.arcadiacore.data.DataManager;
import net.arcadia.arcadiacore.towny.ArcadianTownyManager;
import net.arcadia.arcadiacore.towny.ArcadianTownyManager.CurrencyInfo;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DepositCommand implements CommandExecutor, TabCompleter {

    private final ArcadiaCore plugin;
    private final ArcadianTownyManager manager;

    public DepositCommand(ArcadiaCore plugin, ArcadianTownyManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <goldAmount> <currency>");
            return true;
        }

        // ----- Parse gold amount -----
        int goldAmount;
        try {
            goldAmount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid gold amount: " + args[0]);
            return true;
        }

        if (goldAmount <= 0) {
            player.sendMessage(ChatColor.RED + "Amount must be positive.");
            return true;
        }

        // ----- Currency name & validation -----
        String currencyName = args[1];
        String currencyKey = currencyName.toLowerCase(Locale.ROOT);

        CurrencyInfo matched = null;
        for (CurrencyInfo ci : manager.getAllCurrencies()) {
            if (ci.name.equalsIgnoreCase(currencyName)) {
                matched = ci;
                break;
            }
        }

        if (matched == null) {
            player.sendMessage(ChatColor.RED + "Unknown currency: " + currencyName);
            player.sendMessage(ChatColor.YELLOW + "Use /eco currencylist to see all approved currencies.");
            return true;
        }

        double standard = matched.standard; // how much of this currency per 1 gold (your chosen meaning)

        if (standard <= 0.0) {
            player.sendMessage(ChatColor.RED + "Currency " + matched.name + " has an invalid standard value.");
            return true;
        }

        // ----- Check player has enough gold ingots -----
        int goldOnHand = countMaterial(player, Material.GOLD_INGOT);

        if (goldOnHand < goldAmount) {
            player.sendMessage(ChatColor.RED + "You only have " + goldOnHand + " gold ingots.");
            return true;
        }

        // ----- Remove gold from inventory -----
        removeMaterial(player, Material.GOLD_INGOT, goldAmount);

        // ----- Credit ONLY the requested currency in wallet -----
        double credited = goldAmount * standard;

        DataManager dm = plugin.getDataManager();
        ArcPlayerData pd = dm.getOrCreate(player.getUniqueId(), player.getName());
        pd.addBalance(matched.name, credited);
        dm.saveAll();

        player.sendMessage(
                ChatColor.GREEN + "Deposited "
                        + ChatColor.AQUA + goldAmount + " gold ingots"
                        + ChatColor.GREEN + " into "
                        + ChatColor.AQUA + matched.name
                        + ChatColor.GREEN + ". You received "
                        + ChatColor.AQUA + String.format(Locale.US, "%.2f", credited)
                        + " " + matched.name + ChatColor.GREEN + "."
        );

        return true;
    }

    // ===== Helper: count & remove gold =====

    private int countMaterial(Player player, Material mat) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == mat) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private void removeMaterial(Player player, Material mat, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != mat) continue;

            int stackAmt = stack.getAmount();
            if (stackAmt <= remaining) {
                remaining -= stackAmt;
                contents[i] = null;
            } else {
                stack.setAmount(stackAmt - remaining);
                remaining = 0;
            }
        }

        player.getInventory().setContents(contents);
        player.updateInventory();
    }

    // ===== TAB COMPLETE =====

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {

        if (args.length == 1) {
            // common gold amounts
            return prefixFilter(args[0], List.of("1", "8", "16", "32", "64"));
        }

        if (args.length == 2) {
            // suggest approved currencies
            List<String> names = new ArrayList<>();
            for (CurrencyInfo ci : manager.getAllCurrencies()) {
                names.add(ci.name);
            }
            return prefixFilter(args[1], names);
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
