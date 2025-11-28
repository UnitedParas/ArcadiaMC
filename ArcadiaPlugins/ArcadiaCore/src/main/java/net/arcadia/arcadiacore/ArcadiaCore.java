package net.arcadia.arcadiacore;

import net.arcadia.arcadiacore.commands.*;
import net.arcadia.arcadiacore.data.DataManager;
import net.arcadia.arcadiacore.data.RankManager;
import net.arcadia.arcadiacore.listeners.ArcJoinListener;
import net.arcadia.arcadiacore.commands.SailCommand;
import net.arcadia.arcadiacore.sail.SailListener;
import net.arcadia.arcadiacore.sail.SailStateManager;
import net.arcadia.arcadiacore.towny.ArcadianTownyManager;
import net.arcadia.arcadiacore.towny.ArcadianTownyModule;
import net.arcadia.arcadiacore.towny.commands.AdEcoCommand;
import net.arcadia.arcadiacore.towny.commands.EcoCommand;
import net.arcadia.arcadiacore.towny.commands.NecoCommand;
import net.arcadia.arcadiacore.towny.commands.PriestCommand;
import net.arcadia.arcadiacore.towny.commands.TAdminReligionCommand;
import net.arcadia.arcadiacore.towny.trade.TownTradeManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class ArcadiaCore extends JavaPlugin {

    private static ArcadiaCore instance;

    private DataManager dataManager;
    private RankManager rankManager;
    private SailStateManager sailStateManager;
    private TownTradeManager townTradeManager;

    // Arcadian Towny addon
    private ArcadianTownyModule arcadianTownyModule;

    // Vault / VaultUnlocked economy (standard ₳)
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;

        // Ensure plugin folder exists
        if (!getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            getDataFolder().mkdirs();
        }

        // Hook Vault / VaultUnlocked economy for ₳ standard
        setupEconomy();

        this.dataManager = new DataManager(this);
        this.rankManager = new RankManager(dataManager);
        this.sailStateManager = new SailStateManager();
        this.townTradeManager = new TownTradeManager(this);

        // ====== Commands Setup (non-Towny) ======

        // /arc
        ArcCommand arcCommand = new ArcCommand(this);
        PluginCommand arc = getCommand("arc");
        if (arc != null) {
            arc.setExecutor(arcCommand);
            arc.setTabCompleter(arcCommand);
        }

        // /arcad
        ArcAdminCommand arcAdminCommand = new ArcAdminCommand(this);
        PluginCommand arcad = getCommand("arcad");
        if (arcad != null) {
            arcad.setExecutor(arcAdminCommand);
            arcad.setTabCompleter(arcAdminCommand);
        }

        // /emote
        EmoteCommand emoteCommand = new EmoteCommand(this, arcCommand);
        PluginCommand emote = getCommand("emote");
        if (emote != null) {
            emote.setExecutor(emoteCommand);
            emote.setTabCompleter(emoteCommand);
        }

        // /ademote
        AdEmoteCommand adEmoteCommand = new AdEmoteCommand(this);
        PluginCommand ademote = getCommand("ademote");
        if (ademote != null) {
            ademote.setExecutor(adEmoteCommand);
            ademote.setTabCompleter(adEmoteCommand);
        }

        // /modcall
        ModcallCommand modcallCommand = new ModcallCommand(this, arcCommand);
        PluginCommand modcall = getCommand("modcall");
        if (modcall != null) {
            modcall.setExecutor(modcallCommand);
            modcall.setTabCompleter(modcallCommand);
        }

        // /shika
        PluginCommand shika = getCommand("shika");
        if (shika != null) {
            shika.setExecutor(new ShikaCommand());
        }

        // /sail
        PluginCommand sail = getCommand("sail");
        if (sail != null) {
            SailCommand sailCommand = new SailCommand(this, sailStateManager);
            sail.setExecutor(sailCommand);
            sail.setTabCompleter(sailCommand);
        }

        // /wallet (no Towny dependency, uses Vault + YAML wallet)
        PluginCommand wallet = getCommand("wallet");
        if (wallet != null) {
            WalletCommand walletCommand = new WalletCommand(this);
            wallet.setExecutor(walletCommand);
            wallet.setTabCompleter(walletCommand);
        }

        // ====== Listeners ======
        getServer().getPluginManager().registerEvents(new ArcJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new SailListener(sailStateManager), this);

        // ====== Arcadian Towny Addon (Towny-dependent commands) ======
        setupArcadianTowny();

        getLogger().info("ArcadiaCore enabled.");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAll();
        }
        getLogger().info("ArcadiaCore disabled.");
    }

    /**
     * Hook Vault / VaultUnlocked economy so we have a standard currency (₳)
     * that Towny/Essentials/etc use.
     */
    private void setupEconomy() {
        // Support either Vault or VaultUnlocked providing an Economy service
        if (getServer().getPluginManager().getPlugin("Vault") == null
                && getServer().getPluginManager().getPlugin("VaultUnlocked") == null) {
            getLogger().warning("[ArcadiaCore] No Vault/VaultUnlocked found; ₳ standard is disabled.");
            return;
        }

        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("[ArcadiaCore] No Economy provider registered; ₳ standard is disabled.");
            return;
        }

        this.economy = rsp.getProvider();
        if (this.economy != null) {
            getLogger().info("[ArcadiaCore] Hooked economy provider: " + this.economy.getName());
        } else {
            getLogger().warning("[ArcadiaCore] Economy provider is null; ₳ standard is disabled.");
        }
    }

    private void setupArcadianTowny() {
        Plugin towny = getServer().getPluginManager().getPlugin("Towny");

        if (towny == null || !towny.isEnabled()) {
            getLogger().warning("[ArcadiaCore] Towny not found. Arcadian Towny addon disabled.");
            return;
        }

        this.arcadianTownyModule = new ArcadianTownyModule(this);
        arcadianTownyModule.registerTownyCommands(); // your Town-based /t commands etc.

        ArcadianTownyManager manager = arcadianTownyModule.getManager();

        // /priest
        PluginCommand priest = getCommand("priest");
        if (priest != null) {
            priest.setExecutor(new PriestCommand(manager));
        }

        // /neco
        PluginCommand neco = getCommand("neco");
        if (neco != null) {
            neco.setExecutor(new NecoCommand(manager));
        }

        // /eco  (global economics / currencies)
        PluginCommand eco = getCommand("eco");
        if (eco != null) {
            eco.setExecutor(new EcoCommand(manager));
        }

        // /adeco
        PluginCommand adeco = getCommand("adeco");
        if (adeco != null) {
            adeco.setExecutor(new AdEcoCommand(manager));
        }

        // /tadmin
        PluginCommand tadmin = getCommand("tadmin");
        if (tadmin != null) {
            tadmin.setExecutor(new TAdminReligionCommand(manager));
        }

        // /deposit  (needs manager + Vault + Towny)
        PluginCommand deposit = getCommand("deposit");
        if (deposit != null) {
            deposit.setExecutor(new DepositCommand(this, manager));
        }

        getLogger().info("[ArcadiaCore] Arcadian Towny addon enabled.");
    }

    public static ArcadiaCore getInstance() {
        return instance;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public RankManager getRankManager() {
        return rankManager;
    }

    public SailStateManager getSailStateManager() {
        return sailStateManager;
    }

    public TownTradeManager getTownTradeManager() {
        return townTradeManager;
    }

    public ArcadianTownyModule getArcadianTownyModule() {
        return arcadianTownyModule;
    }

    /**
     * Global access to the Vault/VaultUnlocked Economy object (₳ standard).
     */
    public Economy getEconomy() {
        return economy;
    }
}
