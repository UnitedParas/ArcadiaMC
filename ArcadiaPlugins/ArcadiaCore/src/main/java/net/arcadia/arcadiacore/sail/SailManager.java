package net.arcadia.arcadiacore.sail;

import net.arcadia.arcadiacore.ArcadiaCore;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SailManager {

    private final ArcadiaCore plugin;

    // Who is currently sailing
    private final Set<UUID> sailing = new HashSet<>();
    // Per-player repeating tasks
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    public SailManager(ArcadiaCore plugin) {
        this.plugin = plugin;
    }

    public boolean isSailing(UUID uuid) {
        return sailing.contains(uuid);
    }

    public void startSailing(Player player) {
        UUID uuid = player.getUniqueId();
        if (sailing.contains(uuid)) return;

        sailing.add(uuid);

        BukkitTask task = new BukkitRunnable() {
            int tickCounter = 0; // for hunger every 5s

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    stopSailing(player, "");
                    return;
                }

                Boat boat = getBoat(player);
                if (boat == null || !isBoatOnWater(boat)) {
                    stopSailing(player, ChatColor.RED + "You are no longer in a boat on water. Sailing stopped.");
                    return;
                }

                // BOOST
                Vector vel = boat.getVelocity();
                if (vel.lengthSquared() < 0.01) {
                    // Compute forward vector from yaw
                    float yaw = boat.getLocation().getYaw();
                    double rad = Math.toRadians(yaw);
                    double x = -Math.sin(rad);
                    double z = Math.cos(rad);
                    vel = new Vector(x, 0, z).multiply(0.6);
                } else {
                    vel = vel.multiply(1.2);
                }
                boat.setVelocity(vel);

                // HUNGER every 5 seconds (100 ticks if we run every 5 ticks)
                tickCounter += 5;
                if (tickCounter >= 100) {
                    tickCounter = 0;
                    int food = player.getFoodLevel();
                    if (food > 0) {
                        player.setFoodLevel(food - 1);
                    } else {
                        // no more food, stop sailing
                        stopSailing(player, ChatColor.RED + "You are too exhausted to keep sailing.");
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L); // every 5 ticks

        tasks.put(uuid, task);
        player.sendMessage(ChatColor.AQUA + "You begin sailing!");
    }

    public void stopSailing(Player player, String reason) {
        UUID uuid = player.getUniqueId();
        sailing.remove(uuid);
        BukkitTask task = tasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        if (reason != null && !reason.isEmpty()) {
            player.sendMessage(reason);
        } else {
            player.sendMessage(ChatColor.YELLOW + "You stop sailing.");
        }
    }

    public void shutdown() {
        for (BukkitTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
        sailing.clear();
    }

    public Boat getBoat(Player player) {
        if (player.getVehicle() instanceof Boat boat) {
            return boat;
        }
        return null;
    }

    public boolean isBoatOnWater(Boat boat) {
        Material m = boat.getLocation().getBlock().getType();
        return m == Material.WATER || m == Material.KELP || m == Material.SEAGRASS;
    }
}
