package net.arcadia.arcadiacore.listeners;

import net.arcadia.arcadiacore.ArcadiaCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ArcJoinListener implements Listener {

    private final ArcadiaCore plugin;

    public ArcJoinListener(ArcadiaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var p = event.getPlayer();
        plugin.getDataManager().getOrCreate(p.getUniqueId(), p.getName());
    }
}
