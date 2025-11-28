package net.arcadia.arcadiacore.towny.trade;

import net.arcadia.arcadiacore.ArcadiaCore;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles storage and lookup of town export listings.
 *
 * Data layout in config.yml:
 *
 * trades:
 *   exports:
 *     TownName:
 *       DIAMOND:
 *         amount: 128
 *         price: 5.0
 *         per: 16
 *         nation: MyNation
 */
public class TownTradeManager {

    private static final String ROOT = "trades.exports";

    private final ArcadiaCore plugin;

    public TownTradeManager(ArcadiaCore plugin) {
        this.plugin = plugin;
    }

    public ArcadiaCore getPlugin() {
        return plugin;
    }

    public static final class ExportListing {
        public final String townName;
        public final String nationName; // may be null
        public final Material material;
        public final int amount;
        public final double price;
        public final int per;

        public ExportListing(String townName,
                             String nationName,
                             Material material,
                             int amount,
                             double price,
                             int per) {
            this.townName = townName;
            this.nationName = nationName;
            this.material = material;
            this.amount = amount;
            this.price = price;
            this.per = per;
        }
    }

    private String basePath(String townName, Material material) {
        return ROOT + "." + townName + "." + material.name();
    }

    /**
     * Adds (or increases) an export listing for a town.
     * If a listing already exists for this material, its amount is increased,
     * and price/per are overwritten with the latest values.
     */
    public boolean addOrUpdateExport(String townName,
                                     String nationName,
                                     Material material,
                                     int amount,
                                     double price,
                                     int per) {
        if (amount <= 0 || price <= 0.0 || per <= 0) {
            return false;
        }

        FileConfiguration cfg = plugin.getConfig();
        String base = basePath(townName, material);

        int current = cfg.getInt(base + ".amount", 0);
        cfg.set(base + ".amount", current + amount);
        cfg.set(base + ".price", price);
        cfg.set(base + ".per", per);
        if (nationName != null) {
            cfg.set(base + ".nation", nationName);
        }

        plugin.saveConfig();
        return true;
    }

    /**
     * Decrease a town's export amount for a given material.
     * If there is not enough amount, returns false and does not change config.
     * If the resulting amount is zero, the listing is removed.
     */
    public boolean decreaseExportAmount(String townName,
                                        Material material,
                                        int amount) {
        if (amount <= 0) {
            return false;
        }

        FileConfiguration cfg = plugin.getConfig();
        String base = basePath(townName, material);

        int current = cfg.getInt(base + ".amount", 0);
        if (current < amount || current <= 0) {
            return false;
        }

        int remaining = current - amount;
        if (remaining > 0) {
            cfg.set(base + ".amount", remaining);
        } else {
            // remove listing entirely
            cfg.set(base, null);
        }

        plugin.saveConfig();
        return true;
    }

    /**
     * Completely removes a town's export listing for a material.
     */
    public void clearExport(String townName, Material material) {
        FileConfiguration cfg = plugin.getConfig();
        String base = basePath(townName, material);
        cfg.set(base, null);
        plugin.saveConfig();
    }

    /**
     * All export listings for a single town.
     */
    public List<ExportListing> getExportsForTown(String townName) {
        List<ExportListing> out = new ArrayList<>();

        FileConfiguration cfg = plugin.getConfig();
        String townPath = ROOT + "." + townName;
        ConfigurationSection sec = cfg.getConfigurationSection(townPath);
        if (sec == null) {
            return out;
        }

        for (String matKey : sec.getKeys(false)) {
            Material mat;
            try {
                mat = Material.valueOf(matKey.toUpperCase());
            } catch (IllegalArgumentException ex) {
                continue;
            }

            String base = townPath + "." + matKey;
            int amount = cfg.getInt(base + ".amount", 0);
            double price = cfg.getDouble(base + ".price", 0.0);
            int per = cfg.getInt(base + ".per", 0);
            String nation = cfg.getString(base + ".nation", null);

            if (amount <= 0 || price <= 0.0 || per <= 0) {
                continue;
            }

            out.add(new ExportListing(townName, nation, mat, amount, price, per));
        }

        return out;
    }

    /**
     * All export listings across every town.
     */
    public List<ExportListing> getAllExports() {
        List<ExportListing> out = new ArrayList<>();

        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection rootSec = cfg.getConfigurationSection(ROOT);
        if (rootSec == null) {
            return out;
        }

        for (String townName : rootSec.getKeys(false)) {
            out.addAll(getExportsForTown(townName));
        }

        return out;
    }

    /**
     * Removes export listings for a given material from a town.
     * Returns how many listings were removed (0 or 1 with the current storage layout).
     */
    public int removeExportsByMaterial(String townName, Material material) {
        FileConfiguration cfg = plugin.getConfig();
        String base = basePath(townName, material);

        if (!cfg.contains(base)) {
            return 0;
        }

        cfg.set(base, null);
        plugin.saveConfig();
        return 1;
    }
}
