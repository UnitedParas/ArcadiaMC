package net.arcadia.arcadiacore.towny;

import net.arcadia.arcadiacore.ArcadiaCore;
import net.arcadia.arcadiacore.towny.commands.NationSpiritCommand;
import net.arcadia.arcadiacore.towny.commands.NationElectCommand;
import net.arcadia.arcadiacore.towny.commands.NationPolMarriageCommand;
import org.bukkit.plugin.Plugin;

import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;

public class ArcadianTownyModule {

    private final ArcadiaCore plugin;
    private final ArcadianTownyManager manager;

    public ArcadianTownyModule(ArcadiaCore plugin) {
        this.plugin = plugin;
        this.manager = new ArcadianTownyManager(plugin);
    }

    public ArcadianTownyManager getManager() {
        return manager;
    }

    public void registerTownyCommands() {
        Plugin towny = plugin.getServer().getPluginManager().getPlugin("Towny");
        if (towny == null || !towny.isEnabled()) {
            plugin.getLogger().warning("[ArcadianTowny] Towny not found or not enabled, skipping Towny command registration.");
            return;
        }

        // /n spirit
        TownyCommandAddonAPI.addSubCommand(
                CommandType.NATION,
                "spirit",
                new NationSpiritCommand(plugin, manager)
        );

        // /n elect
        TownyCommandAddonAPI.addSubCommand(
                CommandType.NATION,
                "elect",
                new NationElectCommand(manager)
        );

        // /n polmarriage
        TownyCommandAddonAPI.addSubCommand(
                CommandType.NATION,
                "polmarriage",
                new NationPolMarriageCommand(manager)
        );

        plugin.getLogger().info("[ArcadianTowny] Registered /n spirit, /n elect, /n polmarriage.");
    }
}
