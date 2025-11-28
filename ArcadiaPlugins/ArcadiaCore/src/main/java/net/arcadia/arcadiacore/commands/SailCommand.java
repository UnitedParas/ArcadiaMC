package net.arcadia.arcadiacore.commands;

import net.arcadia.arcadiacore.ArcadiaCore;
import net.arcadia.arcadiacore.data.RankManager;
import net.arcadia.arcadiacore.sail.SailChanneler;
import net.arcadia.arcadiacore.sail.SailStateManager;
import net.arcadia.arcadiacore.sail.SailStateManager.Mode;
import net.arcadia.arcadiacore.sail.SailBoostUtil;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class SailCommand implements CommandExecutor, TabCompleter {

    private final ArcadiaCore plugin;
    private final RankManager rankManager;
    private final SailStateManager state;

    public SailCommand(ArcadiaCore plugin, SailStateManager state) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
        this.state = state;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(ChatColor.RED + "Only players can use /sail.");
            return true;
        }

        // Permission / rank check: {{Sail}} uses internal key "sail"
        if (!rankManager.canUse(p, "sail", 2)) { // Player (2) or higher
            p.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        SailStateManager.SailState s = state.get(p.getUniqueId());

        // If already boosting, stop it and inform
        if (s.mode == Mode.BOOSTING) {
            Boat boat = (p.getVehicle() instanceof Boat b) ? b : null;
            SailBoostUtil.disableBoostIfAny(boat, s);
            s.mode = Mode.IDLE;
            p.sendMessage("§eSailing ended.");
            return true;
        }

        // If channeling, cancel
        if (s.mode == Mode.CHANNELING) {
            SailChanneler.cancelChannel(p, s, "§cSailing canceled.");
            return true;
        }

        // Must be in a boat AND in water to start channeling
        if (!(p.getVehicle() instanceof Boat boat)) {
            p.sendMessage("§cYou must be in a boat to sail!");
            return true;
        }
        if (!boat.isInWater()) {
            p.sendMessage("§cYou must be in water to sail!");
            return true;
        }

        // Start channel
        SailChanneler.startChannel(p, boat, s);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
