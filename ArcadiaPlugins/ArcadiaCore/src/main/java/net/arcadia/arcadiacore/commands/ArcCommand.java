package net.arcadia.arcadiacore.commands;

import net.arcadia.arcadiacore.ArcadiaCore;
import net.arcadia.arcadiacore.data.ArcPlayerData;
import net.arcadia.arcadiacore.data.RankManager;
import net.md_5.bungee.api.ChatColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class ArcCommand implements CommandExecutor, TabCompleter {

    private final ArcadiaCore plugin;
    private final RankManager rankManager;

    private final Map<String, SubcommandHandler> subcommands = new HashMap<>();

    public ArcCommand(ArcadiaCore plugin) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();

        register("help", this::handleHelp);
        register("emote", this::handleEmote);
        register("modcall", this::handleModcall);
        register("data", this::handleData);
    }

    @FunctionalInterface
    public interface SubcommandHandler {
        boolean execute(CommandSender sender, String label, String[] args);
    }

    private void register(String name, SubcommandHandler handler) {
        subcommands.put(name.toLowerCase(), handler);
    }

    // ========== GRADIENT PREFIX ==========

    private static String color(String hex, String text) {
        return net.md_5.bungee.api.ChatColor.of(hex) + text;
    }

    private static String arcadiaPrefix() {
        return "" + ChatColor.BOLD
                + color("#13f179", "A")
                + color("#00f595", "r")
                + color("#00f8af", "c")
                + color("#00fbc6", "a")
                + color("#00fddc", "d")
                + color("#00feef", "i")
                + color("#00ffff", "a")
                + ChatColor.RESET + ChatColor.AQUA + ": ";
    }

    // ========== EXECUTOR ==========

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length == 0) {
            // /arc -> show help, /arcadia -> just usage
            if (label.equalsIgnoreCase("arcadia")) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /arcadia help");
                return true;
            }
            return handleHelp(sender, label, new String[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        SubcommandHandler handler = subcommands.get(sub);
        if (handler == null) {
            sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " help.");
            return true;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return handler.execute(sender, label, subArgs);
    }

    // ========== HELP ==========

    public boolean handleHelp(CommandSender sender, String label, String[] args) {
        sender.sendMessage(arcadiaPrefix() + ChatColor.AQUA + "This is the help menu!");
        sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " help");
        sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " emote <kiss|mog|smack> [player]");
        sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " modcall");
        sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " data <player>");
        sender.sendMessage(ChatColor.YELLOW + "Usage: /emote <kiss|mog|smack> [player]");
        sender.sendMessage(ChatColor.YELLOW + "Usage: /modcall");
        return true;
    }

    // ========== EMOTE ==========

    public boolean handleEmote(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use emotes.");
            return true;
        }

        if (!rankManager.canUse(player, "emote", 2)) {
            player.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " emote <kiss|mog|smack> [player]");
            return true;
        }

        String emote = args[0].toLowerCase(Locale.ROOT);
        String targetName = args.length >= 2 ? args[1] : null;

        return switch (emote) {
            case "kiss" -> Emotes.kiss(plugin, player, targetName);
            case "mog" -> Emotes.mog(plugin, player, targetName);
            case "smack" -> Emotes.smack(plugin, player, targetName);
            default -> {
                player.sendMessage(ChatColor.RED + "Unknown emote. Try kiss/mog/smack.");
                yield true;
            }
        };
    }

    // ========== MODCALL ==========

    public boolean handleModcall(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use modcall.");
            return true;
        }

        if (!rankManager.canUse(player, "modcall", 2)) {
            player.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        var dataManager = plugin.getDataManager();
        ArcPlayerData pd = dataManager.getOrCreate(player.getUniqueId(), player.getName());

        if (pd.isModcallMuted()) {
            player.sendMessage(ChatColor.RED + "You cannot use /modcall while your modcalls are muted.");
            return true;
        }

        pd.addModcalls(1);
        dataManager.saveAll();

        String playerName = player.getName();

        // Notify staff (rank >=5, not muted) with clickable [Answer]
        plugin.getServer().getOnlinePlayers().forEach(p -> {
            if (rankManager.hasRankAtLeast(p.getUniqueId(), p.getName(), 5)) {
                ArcPlayerData staffData = dataManager.getOrCreate(p.getUniqueId(), p.getName());
                if (!staffData.isModcallMuted()) {
                    Component msg = Component.text(ChatColor.AQUA + playerName + ChatColor.RESET + " asked for assistance! ")
                            .append(Component.text("[Answer]")
                                    .color(TextColor.fromHexString("#00ffff"))
                                    .clickEvent(ClickEvent.runCommand("/tp " + playerName)));
                    p.sendMessage(msg);
                }
            }
        });

        player.sendMessage(ChatColor.GREEN + "Modcall sent! Staff have been notified.");
        return true;
    }

    // ========== DATA VIEW ==========

    public boolean handleData(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!rankManager.canUse(player, "data", 4)) {
            player.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " data <player>");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayerExact(targetName);
        java.util.UUID uuid;
        if (target != null) {
            uuid = target.getUniqueId();
            targetName = target.getName();
        } else {
            // Offline lookup
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
            uuid = off.getUniqueId();
            if (off.getName() != null) {
                targetName = off.getName();
            }
        }

        var dataManager = plugin.getDataManager();
        ArcPlayerData pd = dataManager.getOrCreate(uuid, targetName);

        sender.sendMessage(ChatColor.AQUA + targetName + "'s Data:");
        sender.sendMessage(ChatColor.AQUA + "Server rank: " + ChatColor.WHITE + pd.getRank().name());
        sender.sendMessage(ChatColor.AQUA + "Modcalls: " + ChatColor.WHITE + pd.getModcalls());
        sender.sendMessage(ChatColor.AQUA + "Grants: " + ChatColor.WHITE + String.join(", ", pd.getGrants()));
        sender.sendMessage(ChatColor.AQUA + "Revokes: " + ChatColor.WHITE + String.join(", ", pd.getRevokes()));

        return true;
    }

    // ========== TAB COMPLETE ==========

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        // First arg: only show subcommands you can actually use
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("help"); // everyone

            if (sender instanceof Player p) {
                if (rankManager.canUse(p, "emote", 2)) options.add("emote");
                if (rankManager.canUse(p, "modcall", 2)) options.add("modcall");
                if (rankManager.canUse(p, "data", 4)) options.add("data");
            } else {
                // console sees all
                options.addAll(List.of("emote", "modcall", "data"));
            }

            return filterPrefix(args[0], options);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("emote")) {
            return tabCompleteEmote(sender, Arrays.copyOfRange(args, 1, args.length));
        }

        if (sub.equals("modcall")) {
            return tabCompleteModcall(sender, Arrays.copyOfRange(args, 1, args.length));
        }

        if (sub.equals("data")) {
            if (args.length == 2) {
                if (sender instanceof Player p && !rankManager.canUse(p, "data", 4)) {
                    return Collections.emptyList();
                }
                return null; // player names
            }
        }

        return Collections.emptyList();
    }

    public List<String> tabCompleteEmote(CommandSender sender, String[] args) {
        if (sender instanceof Player p && !rankManager.canUse(p, "emote", 2)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterPrefix(args[0], List.of("kiss", "mog", "smack"));
        } else if (args.length == 2) {
            return null; // player names
        }
        return Collections.emptyList();
    }

    public List<String> tabCompleteModcall(CommandSender sender, String[] args) {
        if (sender instanceof Player p && !rankManager.canUse(p, "modcall", 2)) {
            return Collections.emptyList();
        }
        return Collections.emptyList(); // no extra args anyway
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

    // ========== Inner static for emote behavior ==========

    private static class Emotes {

        // /emote kiss [player] (2-block range)
        public static boolean kiss(ArcadiaCore plugin, Player user, String maybeTarget) {
            Player target = findTarget(user, maybeTarget, 2.0, true, "There's nobody to kiss.");
            if (target == null) return true;

            if (target.equals(user)) {
                user.sendMessage(ChatColor.RED + "No.");
                return true;
            }

            if (!sameWorld(user, target)) {
                user.sendMessage(ChatColor.RED + "They seem to be outside of your reach...");
                return true;
            }

            target.sendActionBar(Component.text(user.getName() + " kissed you!").color(TextColor.color(0xFF69B4)));
            user.getWorld().spawnParticle(
                    org.bukkit.Particle.HEART,
                    target.getLocation().add(0, 1.5, 0),
                    10,
                    0.5, 0.5, 0.5,
                    0.01
            );
            return true;
        }

        // /emote mog [player] (10-block range, same dimension only)
        public static boolean mog(ArcadiaCore plugin, Player user, String maybeTarget) {
            Player target = findTarget(user, maybeTarget, 10.0, true, "There's nobody to mog.");
            if (target == null) return true;

            if (target.equals(user)) {
                user.sendMessage(ChatColor.RED + "Why..?");
                return true;
            }

            if (!sameWorld(user, target)) {
                user.sendMessage(ChatColor.RED + "You haven't mastered interdimensional mogging yet...");
                return true;
            }

            // If target rank >= 8, fail & maybe kick
            RankManager rankManager = plugin.getRankManager();
            int targetRank = rankManager.getRankValue(target.getUniqueId(), target.getName());
            if (targetRank >= 8) {
                String[] msgs = {
                        "You attempt to mog " + target.getName() + ", but realize your inferiority.",
                        "You can't mog a deity...",
                        "Your body doesn't obey.",
                        "How dare you..."
                };
                String chosen = msgs[new java.util.Random().nextInt(msgs.length)];

                if (chosen.equals("How dare you...")) {
                    user.sendMessage(ChatColor.RED + "How dare you.");
                    Bukkit.broadcastMessage(ChatColor.RED + user.getName() + " has been kicked for attempting to mog a deity!");
                    user.kickPlayer("How dare you.");
                } else {
                    user.sendMessage(ChatColor.RED + chosen);
                }
                return true;
            }

            String[] options = {
                    user.getName() + " mogged you! holy aura loss.",
                    "You got mogged by " + user.getName() + "! RIP",
                    "You've been mogged. Blame " + user.getName() + "."
            };
            String msg = options[new java.util.Random().nextInt(options.length)];
            target.sendActionBar(Component.text(msg).color(TextColor.color(0xAAAAAA)));
            target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        // /emote smack [player] (2-block range)
        public static boolean smack(ArcadiaCore plugin, Player user, String maybeTarget) {
            Player target = findTarget(user, maybeTarget, 2.0, true, "There's nobody to smack.");
            if (target == null) return true;

            if (!sameWorld(user, target)) {
                user.sendMessage(ChatColor.RED + "Your hand fails to connect.");
                return true;
            }

            if (target.equals(user)) {
                user.damage(1.0);
                user.sendMessage(ChatColor.RED + "Why?");
                return true;
            }

            // Knockback without damage
            org.bukkit.util.Vector dir = target.getLocation().toVector()
                    .subtract(user.getLocation().toVector())
                    .normalize()
                    .multiply(0.6)
                    .setY(0.3);
            target.setVelocity(dir);

            target.sendActionBar(Component.text(user.getName() + " smacked you!").color(TextColor.color(0xFFAA00)));
            target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
            return true;
        }

        /**
         * Finds a target player either by exact name or closest within range.
         * Enforces the given range in the *same world*.
         */
        private static Player findTarget(Player user, String name, double range, boolean allowClosest, String noNearbyMessage) {
            double maxDistSq = range * range;

            // Named target
            if (name != null) {
                Player target = Bukkit.getPlayerExact(name);
                if (target == null) {
                    user.sendMessage(ChatColor.RED + "Who?");
                    return null;
                }

                // If not same world, let the caller handle the dimension-specific message
                if (!sameWorld(user, target)) {
                    return target;
                }

                double distSq = target.getLocation().distanceSquared(user.getLocation());
                if (distSq > maxDistSq) {
                    user.sendMessage(ChatColor.RED + noNearbyMessage);
                    return null;
                }
                return target;
            }

            // No explicit name: pick closest within range in same world
            if (!allowClosest) {
                user.sendMessage(ChatColor.RED + "You must specify a target.");
                return null;
            }

            Player closest = null;
            double best = maxDistSq;

            for (Player p : user.getWorld().getPlayers()) {
                if (p.equals(user)) continue;
                double dist = p.getLocation().distanceSquared(user.getLocation());
                if (dist <= best) {
                    best = dist;
                    closest = p;
                }
            }

            if (closest == null) {
                user.sendMessage(ChatColor.RED + noNearbyMessage);
            }

            return closest;
        }

        private static boolean sameWorld(Player a, Player b) {
            return a.getWorld().equals(b.getWorld());
        }
    }
}
