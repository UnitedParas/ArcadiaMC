package net.arcadia.arcadiacore.commands;

import net.arcadia.arcadiacore.ArcadiaCore;
import net.arcadia.arcadiacore.data.ArcPlayerData;
import net.arcadia.arcadiacore.data.Rank;
import net.arcadia.arcadiacore.data.RankManager;
import net.md_5.bungee.api.ChatColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ArcAdminCommand implements CommandExecutor, TabCompleter {

    private final ArcadiaCore plugin;
    private final RankManager rankManager;

    private final Map<String, SubcommandHandler> subcommands = new HashMap<>();

    public ArcAdminCommand(ArcadiaCore plugin) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();

        register("rank", this::handleRank);
        register("cmdperm", this::handleCmdPerm);
        register("mutemodcall", this::handleMuteModcall);
        register("unmutemodcall", this::handleUnmuteModcall);
        register("data", this::handleDataAdmin);
        register("ademote", this::handleAdemote);
        register("addnote", this::handleAddNote);
        register("notes", this::handleNotesView);
    }

    @FunctionalInterface
    public interface SubcommandHandler {
        boolean execute(CommandSender sender, String label, String[] args);
    }

    private void register(String name, SubcommandHandler handler) {
        subcommands.put(name.toLowerCase(Locale.ROOT), handler);
    }

    // ===== Permission helper =====

    private boolean isAdminSender(CommandSender sender) {
        if (sender.isOp()) return true;
        if (sender instanceof Player p) {
            return rankManager.hasRankAtLeast(p.getUniqueId(), p.getName(), 8);
        }
        return true; // console
    }

    // ========== EXECUTOR ==========

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!isAdminSender(sender)) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <rank|cmdperm|mutemodcall|unmutemodcall|data|ademote|addnote|notes>");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        SubcommandHandler handler = subcommands.get(sub);
        if (handler == null) {
            sender.sendMessage(ChatColor.RED + "Unknown admin subcommand. Try: rank, cmdperm, mutemodcall, unmutemodcall, data, ademote, addnote, notes");
            return true;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return handler.execute(sender, label, subArgs);
    }

    // ========== /arcad rank grant|revoke ==========

    @SuppressWarnings("deprecation")
    private boolean handleRank(CommandSender sender, String label, String[] args) {
        // /arcad rank grant [playername] [rank]
        // /arcad rank revoke [playername]

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " rank <grant|revoke> <player> [rank]");
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        String targetName = args[1];

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        var dataManager = plugin.getDataManager();
        ArcPlayerData pd = dataManager.getOrCreate(
                target.getUniqueId(),
                target.getName() != null ? target.getName() : targetName
        );

        if (action.equals("grant")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " rank grant <player> <rank>");
                return true;
            }
            Rank rank = Rank.fromString(args[2]);
            pd.setRank(rank);
            dataManager.saveAll();
            sender.sendMessage(ChatColor.GREEN + "Set " + targetName + "'s rank to " + rank.name() + ".");
        } else if (action.equals("revoke")) {
            pd.setRank(Rank.PLAYER);
            dataManager.saveAll();
            sender.sendMessage(ChatColor.YELLOW + "Reset " + targetName + " to rank PLAYER.");
        } else {
            sender.sendMessage(ChatColor.RED + "Unknown action. Use grant or revoke.");
        }

        return true;
    }

    // ========== /arcad cmdperm grant|revoke ==========

    @SuppressWarnings("deprecation")
    private boolean handleCmdPerm(CommandSender sender, String label, String[] args) {
        // /arcad cmdperm grant [Playername] [cmdname]
        // /arcad cmdperm revoke [Playername] [cmdname]

        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " cmdperm <grant|revoke> <player> <cmdKey>");
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        String targetName = args[1];
        String cmdKey = args[2]; // e.g., emote.kiss or sail

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        var dataManager = plugin.getDataManager();
        ArcPlayerData pd = dataManager.getOrCreate(
                target.getUniqueId(),
                target.getName() != null ? target.getName() : targetName
        );

        // NOTE: ArcPlayerData getters return Set<String>, so we match that.
        Set<String> grants = pd.getGrants();
        Set<String> revokes = pd.getRevokes();

        switch (action) {
            case "grant" -> {
                // If they already have this grant, don't double-add it.
                if (grants.contains(cmdKey)) {
                    sender.sendMessage(ChatColor.RED + targetName + " already has cmdperm key '" + cmdKey + "'.");
                    return true;
                }

                pd.grantCommand(cmdKey);
                sender.sendMessage(ChatColor.GREEN + "Granted " + cmdKey + " to " + targetName + ".");
            }
            case "revoke" -> {
                // If they don't have *any* entry for this key, warn instead of silently doing nothing.
                if (!grants.contains(cmdKey) && !revokes.contains(cmdKey)) {
                    sender.sendMessage(ChatColor.RED + targetName + " has no cmdperm entry for '" + cmdKey + "'.");
                    return true;
                }

                pd.revokeCommand(cmdKey);
                sender.sendMessage(ChatColor.YELLOW + "Revoked " + cmdKey + " from " + targetName + ".");
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown action. Use grant or revoke.");
                return true;
            }
        }

        dataManager.saveAll();
        return true;
    }

    // ========== /arcad mutemodcall / unmutemodcall ==========

    private boolean handleMuteModcall(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can mute their own modcalls.");
            return true;
        }

        var dataManager = plugin.getDataManager();
        ArcPlayerData pd = dataManager.getOrCreate(player.getUniqueId(), player.getName());

        if (pd.isModcallMuted()) {
            player.sendMessage(ChatColor.RED + "Already muted!");
            return true;
        }

        pd.setModcallMuted(true);
        dataManager.saveAll();
        player.sendMessage(ChatColor.GREEN + "Modcalls muted!");
        return true;
    }

    private boolean handleUnmuteModcall(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can unmute their own modcalls.");
            return true;
        }

        var dataManager = plugin.getDataManager();
        ArcPlayerData pd = dataManager.getOrCreate(player.getUniqueId(), player.getName());

        if (!pd.isModcallMuted()) {
            player.sendMessage(ChatColor.RED + "Already unmuted!");
            return true;
        }

        pd.setModcallMuted(false);
        dataManager.saveAll();
        player.sendMessage(ChatColor.GREEN + "Modcalls unmuted!");
        return true;
    }

    // ========== /arcad data ... (admin-side data ops + view) ==========

    @SuppressWarnings("deprecation")
    private boolean handleDataAdmin(CommandSender sender, String label, String[] args) {
        // VIEW:
        //   /arcad data <player>
        //
        // MODIFY (existing behavior):
        //   /arcad data <player> add modcalls <n>
        //   /arcad data <player> remove modcalls <n>
        //   /arcad data <player> reset <modcalls|grants|revokes>

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " data <player> [reset|add|remove] ...");
            return true;
        }

        String targetName = args[0];
        OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
        String realName = off.getName() != null ? off.getName() : targetName;

        var dataManager = plugin.getDataManager();
        ArcPlayerData pd = dataManager.getOrCreate(off.getUniqueId(), realName);

        // --- Simple view: /arcad data <player> ---
        if (args.length == 1) {
            sender.sendMessage(ChatColor.AQUA + realName + "'s Data:");
            sender.sendMessage(ChatColor.AQUA + "UUID: " + ChatColor.WHITE + off.getUniqueId());
            sender.sendMessage(ChatColor.AQUA + "Server rank: " + ChatColor.WHITE + pd.getRank().name());
            sender.sendMessage(ChatColor.AQUA + "Modcalls: " + ChatColor.WHITE + pd.getModcalls());
            sender.sendMessage(ChatColor.AQUA + "Modcall muted: " + ChatColor.WHITE + (pd.isModcallMuted() ? "yes" : "no"));

            String grants = pd.getGrants().isEmpty()
                    ? "(none)"
                    : String.join(", ", pd.getGrants());
            String revokes = pd.getRevokes().isEmpty()
                    ? "(none)"
                    : String.join(", ", pd.getRevokes());

            sender.sendMessage(ChatColor.AQUA + "Grants: " + ChatColor.WHITE + grants);
            sender.sendMessage(ChatColor.AQUA + "Revokes: " + ChatColor.WHITE + revokes);

            // Notes summary
            List<String> notes = pd.getNotes();
            sender.sendMessage(ChatColor.AQUA + "Notes (" + notes.size() + "):");
            if (notes.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "  (no notes)");
            } else {
                int i = 1;
                for (String n : notes) {
                    sender.sendMessage(ChatColor.GRAY + "  " + (i++) + ". " + n);
                }
            }

            return true;
        }

        // --- Existing subcommand behavior below ---

        String sub = args[1].toLowerCase(Locale.ROOT);

        if (sub.equals("reset")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " data <player> reset <modcalls|grants|revokes>");
                return true;
            }
            String what = args[2].toLowerCase(Locale.ROOT);
            switch (what) {
                case "modcalls" -> {
                    pd.setModcalls(0);
                    sender.sendMessage(ChatColor.GREEN + "Reset modcalls for " + realName + ".");
                }
                case "grants" -> {
                    pd.getGrants().clear();
                    sender.sendMessage(ChatColor.GREEN + "Cleared grants for " + realName + ".");
                }
                case "revokes" -> {
                    pd.getRevokes().clear();
                    sender.sendMessage(ChatColor.GREEN + "Cleared revokes for " + realName + ".");
                }
                default -> sender.sendMessage(ChatColor.RED + "Unknown field. Use modcalls, grants, or revokes.");
            }
            dataManager.saveAll();
            return true;
        }

        if (sub.equals("add") || sub.equals("remove")) {
            if (args.length < 4) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " data <player> " + sub + " modcalls <number>");
                return true;
            }
            String field = args[2].toLowerCase(Locale.ROOT);
            if (!field.equals("modcalls")) {
                sender.sendMessage(ChatColor.RED + "Currently only 'modcalls' is supported for add/remove.");
                return true;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[3]);
                if (amount <= 0) {
                    sender.sendMessage(ChatColor.RED + "Number must be positive.");
                    return true;
                }
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Invalid number: " + args[3]);
                return true;
            }

            if (sub.equals("add")) {
                pd.addModcalls(amount);
                sender.sendMessage(ChatColor.GREEN + "Added " + amount + " modcalls to " + realName + ".");
            } else {
                int current = pd.getModcalls();
                if (current - amount < 0) {
                    sender.sendMessage(ChatColor.RED + "Result would be negative. Current modcalls: " + current);
                    return true;
                }
                pd.setModcalls(current - amount);
                sender.sendMessage(ChatColor.GREEN + "Removed " + amount + " modcalls from " + realName + ".");
            }

            dataManager.saveAll();
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Unknown data subcommand. Use reset, add, or remove.");
        return true;
    }

    // ========== /arcad ademote ... (Shinderu, Poof, Smite, Supermog) ==========

    private boolean handleAdemote(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use ademote actions.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " ademote <shinderu|poof|smite|supermog> [player]");
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);

        switch (action) {
            case "shinderu" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " ademote shinderu <player>");
                    return true;
                }
                String targetName = args[1];
                Player target = Bukkit.getPlayerExact(targetName);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Who?");
                    return true;
                }

                // 10-block range check
                if (!target.getWorld().equals(player.getWorld())
                        || target.getLocation().distanceSquared(player.getLocation()) > 10 * 10) {
                    sender.sendMessage(ChatColor.RED + "They are too far away.");
                    return true;
                }

                // Teleport behind target
                var loc = target.getLocation().clone();
                var dir = loc.getDirection().normalize().multiply(-1); // behind
                loc.add(dir.setY(0));
                loc.setYaw(loc.getYaw() + 180f);
                player.teleport(loc);

                // Blindness and actionbar
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false, false));
                target.sendActionBar(Component.text("お前はも、う死んでいる。"));
                return true;
            }

            case "poof" -> {
                // /ademote poof  -> self invis + notify nearby
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 0, false, false, false));
                int radiusSq = 10 * 10;
                for (Player p : player.getWorld().getPlayers()) {
                    if (p.equals(player)) continue;
                    if (p.getLocation().distanceSquared(player.getLocation()) <= radiusSq) {
                        p.sendActionBar(Component.text(player.getName() + " went poof!"));
                    }
                }
                return true;
            }

            case "smite" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " ademote smite <player>");
                    return true;
                }
                String targetName = args[1];
                Player target = Bukkit.getPlayerExact(targetName);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Who?");
                    return true;
                }

                for (int i = 0; i < 10; i++) {
                    target.getWorld().strikeLightningEffect(target.getLocation());
                    target.damage(1.0, player);
                }
                return true;
            }

            case "supermog" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " ademote supermog <player>");
                    return true;
                }
                String targetName = args[1];
                Player target = Bukkit.getPlayerExact(targetName);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Who?");
                    return true;
                }

                // Supermog: infinite range + interdimensional allowed
                String[] msgs = {
                        player.getName() + " interdimensionally mogged you!",
                        "You've been mogged... Interdimensionally."
                };
                String msg = msgs[new java.util.Random().nextInt(msgs.length)];
                target.sendActionBar(Component.text(msg).color(TextColor.color(0xAAAAAA)));
                target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 0.5f);
                return true;
            }

            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown ademote action. Use shinderu, poof, smite, or supermog.");
                return true;
            }
        }
    }

    // ========== /arcad addnote <player> <note...> ==========

    @SuppressWarnings("deprecation")
    private boolean handleAddNote(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " addnote <player> <note...>");
            return true;
        }

        String targetName = args[0];
        String note = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        if (note.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Note cannot be empty.");
            return true;
        }

        OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
        String realName = off.getName() != null ? off.getName() : targetName;

        var dataManager = plugin.getDataManager();
        ArcPlayerData pd = dataManager.getOrCreate(off.getUniqueId(), realName);

        // tag with staff name
        String writer = (sender instanceof Player p) ? p.getName() : "CONSOLE";
        pd.addNote("[" + writer + "] " + note);
        dataManager.saveAll();

        sender.sendMessage(ChatColor.GREEN + "Added note to " + realName + ".");
        return true;
    }

    // ========== /arcad notes <player> ==========

    @SuppressWarnings("deprecation")
    private boolean handleNotesView(CommandSender sender, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " notes <player>");
            return true;
        }

        String targetName = args[0];
        OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
        String realName = off.getName() != null ? off.getName() : targetName;

        var dataManager = plugin.getDataManager();
        ArcPlayerData pd = dataManager.getOrCreate(off.getUniqueId(), realName);

        List<String> notes = pd.getNotes();
        sender.sendMessage(ChatColor.AQUA + realName + "'s Notes:");
        if (notes.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "  (no notes)");
            return true;
        }

        int i = 1;
        for (String n : notes) {
            sender.sendMessage(ChatColor.AQUA + "  " + (i++) + ". " + ChatColor.WHITE + n);
        }

        return true;
    }

    // ========== TAB COMPLETE ==========

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {

        if (!isAdminSender(sender)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> options = List.of("rank", "cmdperm", "mutemodcall", "unmutemodcall",
                    "data", "ademote", "addnote", "notes");
            return filterPrefix(args[0], options);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("rank")) {
            if (args.length == 2) {
                return filterPrefix(args[1], List.of("grant", "revoke"));
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("grant")) {
                return null; // player names
            }
            if (args.length == 4 && args[1].equalsIgnoreCase("grant")) {
                return filterPrefix(args[3], List.of("FULL", "ADMIN", "STAFF", "TRUSTED", "DONOR", "PLAYER", "GUEST", "BANISHED"));
            }
        }

        if (sub.equals("cmdperm")) {
            if (args.length == 2) {
                return filterPrefix(args[1], List.of("grant", "revoke"));
            }
            if (args.length == 3) {
                return null; // player names
            }
            if (args.length == 4) {
                // Some common keys
                return filterPrefix(args[3], List.of("emote", "modcall", "data", "sail"));
            }
        }

        if (sub.equals("ademote")) {
            if (args.length == 2) {
                return filterPrefix(args[1], List.of("shinderu", "poof", "smite", "supermog"));
            }
            if (args.length == 3 && !args[1].equalsIgnoreCase("poof")) {
                return null; // player names
            }
        }

        if (sub.equals("data")) {
            if (args.length == 2) {
                return null; // player names
            }
            if (args.length == 3) {
                return filterPrefix(args[2], List.of("reset", "add", "remove"));
            }
            if (args.length == 4 && args[2].equalsIgnoreCase("reset")) {
                return filterPrefix(args[3], List.of("modcalls", "grants", "revokes"));
            }
            if (args.length == 4 && (args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("remove"))) {
                return filterPrefix(args[3], List.of("modcalls"));
            }
        }

        if (sub.equals("addnote") || sub.equals("notes")) {
            if (args.length == 2) {
                return null; // player names
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
