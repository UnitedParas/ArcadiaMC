package net.arcadia.arcadiacore.commands;

import net.arcadia.arcadiacore.ArcadiaCore;
import net.arcadia.arcadiacore.data.RankManager;
import net.md_5.bungee.api.ChatColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class AdEmoteCommand implements CommandExecutor, TabCompleter {

    private final ArcadiaCore plugin;
    private final RankManager rankManager;

    public AdEmoteCommand(ArcadiaCore plugin) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
    }

    private boolean hasAdminAccess(CommandSender sender) {
        if (!(sender instanceof Player p)) return true;
        if (sender.isOp()) return true;
        return rankManager.getRankValue(p.getUniqueId(), p.getName()) >= 8;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!hasAdminAccess(sender)) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use /" + label + ".");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <shinderu|poof|smite|supermog> [player]");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        String targetName = args.length >= 2 ? args[1] : null;

        return switch (sub) {
            case "shinderu" -> shinderu(player, targetName);
            case "poof" -> poof(player);
            case "smite" -> smite(player, targetName);
            case "supermog" -> supermog(player, targetName);
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown ademote. Try shinderu, poof, smite, supermog.");
                yield true;
            }
        };
    }

    // (rest unchanged – same as previous version)
    // ... shinderu / poof / smite / supermog methods, and tab-complete:

    private boolean shinderu(Player user, String name) {
        if (name == null) {
            user.sendMessage(ChatColor.YELLOW + "Usage: /ademote shinderu <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            user.sendMessage(ChatColor.RED + "Who?");
            return true;
        }
        if (!user.getWorld().equals(target.getWorld())) {
            user.sendMessage(ChatColor.RED + "They seem to be outside of your reach...");
            return true;
        }
        if (user.getLocation().distanceSquared(target.getLocation()) > 100D) {
            user.sendMessage(ChatColor.RED + "They're too far away.");
            return true;
        }

        var loc = target.getLocation().clone();
        loc.setYaw(loc.getYaw() + 180);
        loc.add(loc.getDirection().normalize());
        //FIX. set y to y+1 so user doesnt get stuck in a block lmfao
        loc.setY(target.getLocation().getY() + 1);
        
        user.teleport(loc);


        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false, false));
        target.sendActionBar(Component.text("お前はも、う死んでいる。").color(TextColor.color(0xFF0000)));

        return true;
    }

    private boolean poof(Player user) {
        user.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 0, false, false, false));

        user.getWorld().getPlayers().stream()
                .filter(p -> p.getLocation().distanceSquared(user.getLocation()) <= 100D)
                .forEach(p -> p.sendActionBar(
                        Component.text(user.getName() + " went poof!")
                                .color(TextColor.color(0x00ffff)))
                );

        return true;
    }

    private boolean smite(Player user, String name) {
        if (name == null) {
            user.sendMessage(ChatColor.YELLOW + "Usage: /ademote smite <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            user.sendMessage(ChatColor.RED + "Who?");
            return true;
        }

        for (int i = 0; i < 10; i++) {
            target.getWorld().strikeLightningEffect(target.getLocation());
            target.damage(1.0, user);
        }
        return true;
    }

    private boolean supermog(Player user, String name) {
        if (name == null) {
            user.sendMessage(ChatColor.YELLOW + "Usage: /ademote supermog <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            user.sendMessage(ChatColor.RED + "Who?");
            return true;
        }

        String userName = user.getName();
        String[] msgsSameWorld = {
                userName + " interdimensionally mogged you!",
                "You've been mogged... Interdimensionally."
        };
        String chosen = msgsSameWorld[new Random().nextInt(msgsSameWorld.length)];
        target.sendActionBar(Component.text(chosen).color(TextColor.color(0xAAAAAA)));

        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!hasAdminAccess(sender)) return java.util.Collections.emptyList();

        if (args.length == 1) {
            return filterPrefix(args[0], java.util.List.of("shinderu", "poof", "smite", "supermog"));
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (!sub.equals("poof")) {
                return null; // player names
            }
        }

        return java.util.Collections.emptyList();
    }

    private java.util.List<String> filterPrefix(String token, java.util.List<String> options) {
        String lower = token.toLowerCase(Locale.ROOT);
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String s : options) {
            if (s.toLowerCase(Locale.ROOT).startsWith(lower)) out.add(s);
        }
        return out;
    }
}
