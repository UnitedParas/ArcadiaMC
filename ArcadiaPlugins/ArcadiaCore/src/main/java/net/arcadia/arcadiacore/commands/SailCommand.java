package net.arcadia.arcadiacore.commands;

import net.arcadia.arcadiacore.ArcadiaCore;
import net.arcadia.arcadiacore.data.RankManager;
import net.arcadia.arcadiacore.sail.SailManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.List;

public class SailCommand implements CommandExecutor, TabCompleter {

    private final ArcadiaCore plugin;
    private final RankManager rankManager;
    private final SailManager sailManager;

    public SailCommand(ArcadiaCore plugin) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
        this.sailManager = plugin.getSailManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use /sail.");
            return true;
        }

        // Rank / cmdperm check: {{Sail}} -> internal key "sail"
        if (!rankManager.canUse(player, "sail", 2)) { // Player (2) or higher
            player.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        // Toggle off if already sailing
        if (sailManager.isSailing(player.getUniqueId())) {
            sailManager.stopSailing(player, ChatColor.YELLOW + "You stop sailing.");
            return true;
        }

        // Must be in a boat on water
        Boat boat = sailManager.getBoat(player);
        if (boat == null) {
            player.sendMessage(ChatColor.RED + "You must be in a boat to use /sail.");
            return true;
        }
        if (!sailManager.isBoatOnWater(boat)) {
            player.sendMessage(ChatColor.RED + "Your boat must be on water to use /sail.");
            return true;
        }

        // Channel for 5 seconds
        player.sendMessage(ChatColor.AQUA + "Channeling Sail... (5 seconds)");
        final int channelTicks = 5 * 20;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                // If player left boat during channel, cancel
                if (sailManager.getBoat(player) == null || !sailManager.isBoatOnWater(boat)) {
                    player.sendMessage(ChatColor.RED + "Sail channel interrupted.");
                    cancel();
                    return;
                }

                ticks += 20;
                if (ticks >= channelTicks) {
                    cancel();
                    // Start sailing (if not already toggled off somehow)
                    if (!sailManager.isSailing(player.getUniqueId())) {
                        sailManager.startSailing(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // check each second

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        // No arguments -> no suggestions.
        return Collections.emptyList();
    }
}
