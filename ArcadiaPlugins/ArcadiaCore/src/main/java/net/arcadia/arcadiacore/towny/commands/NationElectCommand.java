package net.arcadia.arcadiacore.towny.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.arcadia.arcadiacore.towny.ArcadianTownyManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

public class NationElectCommand implements CommandExecutor {

    private final ArcadianTownyManager manager;

    public NationElectCommand(ArcadianTownyManager manager) {
        this.manager = manager;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /n elect <priest|vassal|diplomat> <player>");
            return true;
        }

        String role = args[0].toLowerCase(Locale.ROOT);
        String targetName = args[1];

        TownyAPI api = TownyAPI.getInstance();
        Resident res = api.getResident(player);
        if (res == null || !res.hasNation()) {
            player.sendMessage(ChatColor.RED + "You are not in a nation.");
            return true;
        }

        Nation nation = res.getNationOrNull();
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Nation not found.");
            return true;
        }

        // Only king can elect (simple rule)
        if (!res.isKing()) {
            player.sendMessage(ChatColor.RED + "Only the nation leader can use /n elect.");
            return true;
        }

        Player target = player.getServer().getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found or not online.");
            return true;
        }

        Resident targetRes = api.getResident(target);
        if (targetRes == null || !targetRes.hasNation() ||
            !targetRes.getNationOrNull().getName().equals(nation.getName())) {
            player.sendMessage(ChatColor.RED + "That player is not in your nation.");
            return true;
        }

        UUID targetUuid = target.getUniqueId();
        switch (role) {
            case "priest", "diplomat", "vassal" -> {
                manager.setNationRole(nation.getName(), role, targetUuid);
                player.sendMessage(ChatColor.GREEN + "Set " + target.getName() + " as " + role + " of " + nation.getName() + ".");
            }
            default -> player.sendMessage(ChatColor.YELLOW + "Unknown role. Use priest, vassal, or diplomat.");
        }

        return true;
    }
}
