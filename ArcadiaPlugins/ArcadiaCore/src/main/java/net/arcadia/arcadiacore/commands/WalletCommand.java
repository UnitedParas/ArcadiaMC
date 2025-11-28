package net.arcadia.arcadiacore.commands;

import net.arcadia.arcadiacore.ArcadiaCore;
import net.arcadia.arcadiacore.data.ArcPlayerData;
import net.arcadia.arcadiacore.data.DataManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class WalletCommand implements CommandExecutor, TabCompleter {

    private final ArcadiaCore plugin;

    public WalletCommand(ArcadiaCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        DataManager dm = plugin.getDataManager();
        ArcPlayerData pd = dm.getOrCreate(player.getUniqueId(), player.getName());

        Economy econ = plugin.getEconomy();

        player.sendMessage(ChatColor.GOLD + "=== Wallet ===");

        // ===== Standard ₳ (Vault) =====
        if (econ != null) {
            double stdBal = econ.getBalance(player);
            player.sendMessage(ChatColor.AQUA + "₳" + ChatColor.GRAY + ": "
                    + ChatColor.GREEN + String.format(Locale.US, "%.2f", stdBal));
        } else {
            player.sendMessage(ChatColor.RED + "Standard ₳ disabled (no Vault economy provider).");
        }

        // ===== Custom currencies from YAML wallet =====
        Map<String, Double> balances = pd.getWallet();
        if (balances.isEmpty()) {
            player.sendMessage(ChatColor.AQUA + "You have no custom currencies.");
            return true;
        }

        List<Map.Entry<String, Double>> list = new ArrayList<>(balances.entrySet());
        list.sort(Comparator.comparing(e -> e.getKey().toLowerCase(Locale.ROOT)));

        for (Map.Entry<String, Double> e : list) {
            double amt = e.getValue();
            if (amt <= 0.0) continue;

            String cur = e.getKey();
            // Never show "standard" key here even if it exists by mistake
            if (isStandardKey(cur)) continue;

            player.sendMessage(ChatColor.AQUA + cur + ChatColor.GRAY + ": "
                    + ChatColor.GREEN + String.format(Locale.US, "%.2f", amt));
        }

        return true;
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return List.of(); // no args
    }
}
