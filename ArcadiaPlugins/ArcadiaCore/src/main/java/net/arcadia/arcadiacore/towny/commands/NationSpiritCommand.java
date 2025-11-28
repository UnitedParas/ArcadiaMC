package net.arcadia.arcadiacore.towny.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.arcadia.arcadiacore.ArcadiaCore;
import net.arcadia.arcadiacore.towny.ArcadianTownyManager;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class NationSpiritCommand implements CommandExecutor, TabCompleter {

    private final ArcadiaCore plugin;
    private final ArcadianTownyManager manager;

    private static final long REQ_RELIGION_COOLDOWN = 10 * 60 * 1000L; // 10 minutes

    public NationSpiritCommand(ArcadiaCore plugin, ArcadianTownyManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "nationalreligion" -> handleNationalReligion(player, args);
            case "reqreligion" -> handleReqReligion(player, args);
            case "religionlist" -> handleReligionList(player, args);
            default -> sendHelp(player);
        }

        return true;
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

    @SuppressWarnings("deprecation")
    private void handleNationalReligion(Player player, String[] args) {
        Nation nation = getNation(player);
        if (nation == null) return;

        if (args.length < 2) {
            player.sendMessage(ChatColor.GREEN + "Usage: /n spirit nationalreligion <religion>");
            return;
        }

        String religionName = args[1];

        if (!manager.isReligionApproved(religionName)) {
            player.sendMessage(ChatColor.RED + "That religion is not approved yet.");
            return;
        }

        String path = "nations." + nation.getName() + ".nationalReligion";
        plugin.getConfig().set(path, religionName);
        plugin.saveConfig();

        player.sendMessage(ChatColor.GREEN + "Your nation's national religion is now: " + religionName);
    }

    @SuppressWarnings("deprecation")
    private void handleReqReligion(Player player, String[] args) {
        Nation nation = getNation(player);
        if (nation == null) return;

        if (args.length < 2) {
            player.sendMessage(ChatColor.GREEN + "Usage: /n spirit reqreligion <name>");
            return;
        }

        UUID uuid = player.getUniqueId();
        String key = "reqreligion:" + uuid;

        if (manager.isOnCooldown(key, REQ_RELIGION_COOLDOWN)) {
            player.sendMessage(ChatColor.RED + "You must wait before requesting another religion.");
            return;
        }

        String requested = args[1];

        boolean ok = manager.enqueueReligionRequest(uuid.toString(), nation.getName(), requested);
        if (!ok) {
            player.sendMessage(ChatColor.RED + "You already have a pending religion request.");
            return;
        }

        manager.markUsed(key);
        player.sendMessage(ChatColor.GREEN + "Requested new religion \"" + requested + "\" for approval.");
    }

    @SuppressWarnings("deprecation")
    private void handleReligionList(Player player, String[] args) {
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) { }
        }
        if (page < 1) page = 1;

        List<String> religions = manager.getAllReligions();
        int perPage = 10;
        int totalPages = Math.max(1, (int)Math.ceil(religions.size() / (double)perPage));
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, religions.size());

        player.sendMessage(ChatColor.GOLD + "=== Religions (Page " + page + "/" + totalPages + ") ===");
        if (religions.isEmpty()) {
            player.sendMessage(ChatColor.AQUA + "No religions have been approved yet.");
            return;
        }
        for (int i = start; i < end; i++) {
            player.sendMessage(ChatColor.AQUA + "- " + religions.get(i));
        }
    }

    @SuppressWarnings("deprecation")
    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== /n spirit ===");
        player.sendMessage(ChatColor.GREEN + "/n spirit nationalreligion <religion> " +
                ChatColor.AQUA + "- Set your nation's religion.");
        player.sendMessage(ChatColor.GREEN + "/n spirit reqreligion <name> " +
                ChatColor.AQUA + "- Request a new religion (10m cooldown).");
        player.sendMessage(ChatColor.GREEN + "/n spirit religionlist [page] " +
                ChatColor.AQUA + "- View available religions.");
    }

    // ===== TAB COMPLETE =====

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return partialMatch(Arrays.asList("nationalreligion", "reqreligion", "religionlist"), args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("nationalreligion")) {
                return partialMatch(manager.getAllReligions(), args[1]);
            }
            if (sub.equals("religionlist")) {
                return partialMatch(Arrays.asList("1", "2", "3", "4", "5"), args[1]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> partialMatch(Collection<String> base, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String s : base) {
            if (s.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(s);
            }
        }
        return out;
    }
}
