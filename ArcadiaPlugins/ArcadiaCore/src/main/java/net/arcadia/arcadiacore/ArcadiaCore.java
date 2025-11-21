package net.arcadia.arcadiacore;

import net.arcadia.arcadiacore.commands.*;
import net.arcadia.arcadiacore.data.DataManager;
import net.arcadia.arcadiacore.data.RankManager;
import net.arcadia.arcadiacore.listeners.ArcJoinListener;
import net.arcadia.arcadiacore.sail.SailListener;
import net.arcadia.arcadiacore.sail.SailManager;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class ArcadiaCore extends JavaPlugin {

    private static ArcadiaCore instance;

    private DataManager dataManager;
    private RankManager rankManager;
    private SailManager sailManager;

    @Override
    public void onEnable() {
        instance = this;

        // check plugin folder exists
        if (!getDataFolder().exists()) {
            
            getDataFolder().mkdirs();
        }

        this.sailManager = new SailManager(this);

        this.dataManager = new DataManager(this);
        this.rankManager = new RankManager(dataManager);

        // main /arc or /arcadia
        ArcCommand arcCommand = new ArcCommand(this);

        PluginCommand arc = getCommand("arc");
        if (arc != null) {
            arc.setExecutor(arcCommand);
            arc.setTabCompleter(arcCommand);
        }

        // admin root /arcad
        ArcAdminCommand arcAdminCommand = new ArcAdminCommand(this);
        PluginCommand arcad = getCommand("arcad");
        if (arcad != null) {
            arcad.setExecutor(arcAdminCommand);
            arcad.setTabCompleter(arcAdminCommand);
        }

        // root /emote (normal emotes)
        EmoteCommand emoteCommand = new EmoteCommand(this, arcCommand);
        PluginCommand emote = getCommand("emote");
        if (emote != null) {
            emote.setExecutor(emoteCommand);
            emote.setTabCompleter(emoteCommand);
        }

        // root /ademote (admin emotes)
        AdEmoteCommand adEmoteCommand = new AdEmoteCommand(this);
        PluginCommand ademote = getCommand("ademote");
        if (ademote != null) {
            ademote.setExecutor(adEmoteCommand);
            ademote.setTabCompleter(adEmoteCommand);
        }

        // root /modcall
        ModcallCommand modcallCommand = new ModcallCommand(this, arcCommand);
        PluginCommand modcall = getCommand("modcall");
        if (modcall != null) {
            modcall.setExecutor(modcallCommand);
            modcall.setTabCompleter(modcallCommand);
        }

        // root /shika
        PluginCommand shika = getCommand("shika");
        if (shika != null) {
            shika.setExecutor(new ShikaCommand());
        }

         // root /sail
        if (getCommand("sail") != null) {
            SailCommand sailCommand = new SailCommand(this);
            getCommand("sail").setExecutor(sailCommand);
            getCommand("sail").setTabCompleter(sailCommand);
        }

        // join listener (check if players data exists)
        getServer().getPluginManager().registerEvents(new ArcJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new SailListener(this), this);

        getLogger().info("ArcadiaCore enabled.");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAll();
        }
        getLogger().info("ArcadiaCore disabled.");
    }

    public static ArcadiaCore getInstance() {
        return instance;
    }

    


    public DataManager getDataManager() {
        return dataManager;
    }

    public SailManager getSailManager() {
        return sailManager;
    }


    public RankManager getRankManager() {
        return rankManager;
    }
}
