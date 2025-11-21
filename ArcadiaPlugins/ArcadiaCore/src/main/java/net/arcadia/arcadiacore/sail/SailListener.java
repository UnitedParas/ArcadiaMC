package net.arcadia.arcadiacore.sail;

import net.arcadia.arcadiacore.ArcadiaCore;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class SailListener implements Listener {

    private final ArcadiaCore plugin;

    public SailListener(ArcadiaCore plugin) {
        this.plugin = plugin;
    }

    private SailManager manager() {
        return plugin.getSailManager();
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (manager().isSailing(player.getUniqueId())) {
            manager().stopSailing(player, ChatColor.RED + "You were hurt! Sailing interrupted.");
        }
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player player)) return;

        if (manager().isSailing(player.getUniqueId())) {
            manager().stopSailing(player, ChatColor.YELLOW + "You left your boat. Sailing stopped.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (manager().isSailing(player.getUniqueId())) {
            manager().stopSailing(player, "");
        }
    }
}
