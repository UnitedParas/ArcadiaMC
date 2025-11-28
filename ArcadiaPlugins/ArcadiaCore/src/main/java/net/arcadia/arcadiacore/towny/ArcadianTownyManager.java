package net.arcadia.arcadiacore.towny;

import net.arcadia.arcadiacore.ArcadiaCore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class ArcadianTownyManager {

    private final ArcadiaCore plugin;
    private final FileConfiguration config;

    private final Map<String, Long> cooldowns = new HashMap<>();

    public ArcadianTownyManager(ArcadiaCore plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public ArcadiaCore getPlugin() {
        return plugin;
    }

    // ===== Cooldowns =====

    public boolean isOnCooldown(String key, long cooldownMs) {
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(key);
        return last != null && (now - last) < cooldownMs;
    }

    public void markUsed(String key) {
        cooldowns.put(key, System.currentTimeMillis());
    }

    // ===== Religions =====

    public boolean isReligionApproved(String name) {
        List<String> approved = config.getStringList("religions.approved");
        for (String s : approved) {
            if (s.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public boolean enqueueReligionRequest(String playerUuid, String nationName, String requested) {
        String base = "religions.requests." + playerUuid;
        if (config.contains(base + ".name")) {
            return false;
        }
        config.set(base + ".nation", nationName);
        config.set(base + ".name", requested);
        config.set(base + ".time", System.currentTimeMillis());
        plugin.saveConfig();
        return true;
    }

    public List<String> getAllReligions() {
        List<String> approved = new ArrayList<>(config.getStringList("religions.approved"));
        approved.sort(String.CASE_INSENSITIVE_ORDER);
        return approved;
    }

    public static class ReligionRequest {
        public final String requesterUuid;
        public final String nationName;
        public final String religionName;
        public final long time;

        public ReligionRequest(String requesterUuid, String nationName, String religionName, long time) {
            this.requesterUuid = requesterUuid;
            this.nationName = nationName;
            this.religionName = religionName;
            this.time = time;
        }

        public String getRequesterName() {
            try {
                UUID uuid = UUID.fromString(requesterUuid);
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                if (op != null && op.getName() != null) {
                    return op.getName();
                }
            } catch (IllegalArgumentException ignored) {}
            return requesterUuid;
        }
    }

    public List<ReligionRequest> getReligionRequests() {
        ConfigurationSection root = config.getConfigurationSection("religions.requests");
        if (root == null) return Collections.emptyList();

        List<ReligionRequest> list = new ArrayList<>();
        for (String key : root.getKeys(false)) {
            String base = "religions.requests." + key;
            String nation = config.getString(base + ".nation", "Unknown");
            String name = config.getString(base + ".name", "Unknown");
            long time = config.getLong(base + ".time", 0L);
            list.add(new ReligionRequest(key, nation, name, time));
        }
        list.sort(Comparator.comparingLong(r -> r.time));
        return list;
    }

    public boolean approveReligion(String religionName) {
        List<ReligionRequest> pending = getReligionRequests();
        boolean found = false;

        for (ReligionRequest rr : pending) {
            if (rr.religionName.equalsIgnoreCase(religionName)) {
                found = true;
                break;
            }
        }
        if (!found) {
            return false;
        }

        List<String> approved = new ArrayList<>(config.getStringList("religions.approved"));
        boolean already = approved.stream().anyMatch(s -> s.equalsIgnoreCase(religionName));
        if (!already) {
            approved.add(religionName);
            config.set("religions.approved", approved);
        }

        ConfigurationSection root = config.getConfigurationSection("religions.requests");
        if (root != null) {
            for (String key : new ArrayList<>(root.getKeys(false))) {
                String base = "religions.requests." + key;
                String reqName = config.getString(base + ".name");
                if (reqName != null && reqName.equalsIgnoreCase(religionName)) {
                    config.set(base, null);
                }
            }
        }

        plugin.saveConfig();
        return true;
    }

    public List<String> getQueuedReligionNames() {
        List<ReligionRequest> list = getReligionRequests();
        Set<String> names = new LinkedHashSet<>();

        for (ReligionRequest rr : list) {
            names.add(rr.religionName);
        }

        return new ArrayList<>(names);
    }

    // ===== Currencies =====

    public static class CurrencyInfo {
        public final String name;
        public final double standard;
        public final double circulation;

        public CurrencyInfo(String name, double standard, double circulation) {
            this.name = name;
            this.standard = standard;
            this.circulation = circulation;
        }
    }

    public static class CurrencyRequest {
        public final String name;
        public final String nationName;
        public final String requesterUuid;
        public final double standard;
        public final long time;

        public CurrencyRequest(String name, String nationName, String requesterUuid, double standard, long time) {
            this.name = name;
            this.nationName = nationName;
            this.requesterUuid = requesterUuid;
            this.standard = standard;
            this.time = time;
        }
    }

    public boolean enqueueCurrencyRequest(String requesterUuid, String nationName, String currencyName, double standard) {
        String key = currencyName.toLowerCase(Locale.ROOT);
        String base = "currencies.requests." + key;
        if (config.contains(base + ".name")) {
            return false;
        }
        config.set(base + ".name", currencyName);
        config.set(base + ".nation", nationName);
        config.set(base + ".requester", requesterUuid);
        config.set(base + ".standard", standard);
        config.set(base + ".time", System.currentTimeMillis());
        plugin.saveConfig();
        return true;
    }

    public List<CurrencyRequest> getCurrencyRequests() {
        ConfigurationSection root = config.getConfigurationSection("currencies.requests");
        if (root == null) return Collections.emptyList();
        List<CurrencyRequest> list = new ArrayList<>();
        for (String key : root.getKeys(false)) {
            String base = "currencies.requests." + key;
            String name = config.getString(base + ".name", key);
            String nation = config.getString(base + ".nation", "Unknown");
            String requester = config.getString(base + ".requester", "Unknown");
            double standard = config.getDouble(base + ".standard", 1.0);
            long time = config.getLong(base + ".time", 0L);
            list.add(new CurrencyRequest(name, nation, requester, standard, time));
        }
        list.sort(Comparator.comparingLong(r -> r.time));
        return list;
    }

    public List<String> getQueuedCurrencyNames() {
        List<CurrencyRequest> list = getCurrencyRequests();
        Set<String> names = new LinkedHashSet<>();

        for (CurrencyRequest cr : list) {
            names.add(cr.name);
        }

        return new ArrayList<>(names);
    }

    public boolean approveCurrency(String currencyName) {
        String key = currencyName.toLowerCase(Locale.ROOT);
        String reqBase = "currencies.requests." + key;

        if (!config.contains(reqBase + ".name")) {
            return false;
        }

        String displayName = config.getString(reqBase + ".name", currencyName);
        double standard = config.getDouble(reqBase + ".standard", 1.0);

        String base = "currencies.approved." + key;
        config.set(base + ".name", displayName);
        config.set(base + ".standard", standard);
        if (!config.contains(base + ".circulation")) {
            config.set(base + ".circulation", 0.0);
        }

        config.set(reqBase, null);
        plugin.saveConfig();
        return true;
    }

    public boolean rejectCurrency(String currencyName) {
        String key = currencyName.toLowerCase(Locale.ROOT);
        String base = "currencies.requests." + key;
        if (!config.contains(base)) {
            return false;
        }
        config.set(base, null);
        plugin.saveConfig();
        return true;
    }

    public List<CurrencyInfo> getAllCurrencies() {
        ConfigurationSection root = config.getConfigurationSection("currencies.approved");
        if (root == null) return Collections.emptyList();

        List<CurrencyInfo> list = new ArrayList<>();
        for (String key : root.getKeys(false)) {
            String base = "currencies.approved." + key;
            String name = config.getString(base + ".name", key);
            double standard = config.getDouble(base + ".standard", 1.0);
            double circ = config.getDouble(base + ".circulation", 0.0);
            list.add(new CurrencyInfo(name, standard, circ));
        }
        list.sort(Comparator.comparing(c -> c.name.toLowerCase(Locale.ROOT)));
        return list;
    }

    public void setNationCurrency(String nationName, String currencyName) {
        config.set("nations." + nationName + ".currency", currencyName);
        plugin.saveConfig();
    }

    public String getNationCurrency(String nationName) {
        return config.getString("nations." + nationName + ".currency", null);
    }

    public double getCurrencyStandard(String currencyName) {
        String key = currencyName.toLowerCase(Locale.ROOT);
        return config.getDouble("currencies.approved." + key + ".standard", 1.0);
    }

    /**
     * Get the exchange tax (0-100%) for a currency.
     * Stored under: currencies.approved.<currency-lower>.exchangeTax
     */
    public double getNationExchangeTax(String currencyName) {
        if (currencyName == null) return 0.0;
        String key = currencyName.toLowerCase(Locale.ROOT);
        return config.getDouble("currencies.approved." + key + ".exchangeTax", 0.0);
    }

    /**
     * Set the exchange tax (0-100%) for a currency and save it to config.
     * This should be called by your /n currency <name> exchangetax <1-100> command.
     */
    public void setNationExchangeTax(String currencyName, double percent) {
        if (currencyName == null) return;

        if (percent < 0.0) percent = 0.0;
        if (percent > 100.0) percent = 100.0;

        String key = currencyName.toLowerCase(Locale.ROOT);
        config.set("currencies.approved." + key + ".exchangeTax", percent);
        plugin.saveConfig();
    }

    // ------------- NATION ROLES (priest, diplomat, vassals) -------------

    public void setNationRole(String nationName, String role, UUID playerUuid) {
        String base = "nations." + nationName + ".roles.";
        String roleKey = role.toLowerCase(Locale.ROOT);

        if ("vassal".equals(roleKey) || "vassal".equals(roleKey + "s")) {
            List<String> list = config.getStringList(base + "vassals");
            String id = playerUuid.toString();
            if (!list.contains(id)) {
                list.add(id);
            }
            config.set(base + "vassals", list);
        } else if ("priest".equals(roleKey)) {
            config.set(base + "priest", playerUuid.toString());
        } else if ("diplomat".equals(roleKey)) {
            config.set(base + "diplomat", playerUuid.toString());
        }

        plugin.saveConfig();
    }

    public UUID getNationPriest(String nationName) {
        String s = config.getString("nations." + nationName + ".roles.priest", null);
        if (s == null) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isNationPriest(String nationName, UUID uuid) {
        UUID priest = getNationPriest(nationName);
        return priest != null && priest.equals(uuid);
    }

    // ------------- MARRIAGES -------------

    public static class MarriageProposal {
        public final UUID proposer;
        public final UUID target;
        public final long time;

        public MarriageProposal(UUID proposer, UUID target, long time) {
            this.proposer = proposer;
            this.target = target;
            this.time = time;
        }
    }

    public boolean createMarriageProposal(UUID proposer, UUID target) {
        String base = "marriages.proposals." + target.toString();
        if (config.contains(base + ".proposer")) {
            return false; // target already has pending
        }
        config.set(base + ".proposer", proposer.toString());
        config.set(base + ".time", System.currentTimeMillis());
        plugin.saveConfig();
        return true;
    }

    public MarriageProposal getProposalForTarget(UUID target) {
        String base = "marriages.proposals." + target.toString();
        if (!config.contains(base + ".proposer")) {
            return null;
        }
        String proposerStr = config.getString(base + ".proposer");
        long time = config.getLong(base + ".time", 0L);
        try {
            UUID proposer = UUID.fromString(proposerStr);
            return new MarriageProposal(proposer, target, time);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void removeProposal(UUID target) {
        config.set("marriages.proposals." + target.toString(), null);
        plugin.saveConfig();
    }

    public void setEngaged(UUID p1, UUID p2) {
        config.set("marriages.engaged." + p1.toString(), p2.toString());
        config.set("marriages.engaged." + p2.toString(), p1.toString());
        plugin.saveConfig();
    }

    public UUID getEngagedTo(UUID uuid) {
        String other = config.getString("marriages.engaged." + uuid.toString(), null);
        if (other == null) return null;
        try {
            return UUID.fromString(other);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isCurrencyApproved(String currencyName) {
    if (currencyName == null) return false;
    String key = currencyName.toLowerCase(Locale.ROOT);
    return config.isConfigurationSection("currencies.approved." + key);
    }


    public void clearEngagement(UUID p1, UUID p2) {
        config.set("marriages.engaged." + p1.toString(), null);
        config.set("marriages.engaged." + p2.toString(), null);
        plugin.saveConfig();
    }

    public void setMarried(UUID p1, UUID p2) {
        config.set("marriages.married." + p1.toString(), p2.toString());
        config.set("marriages.married." + p2.toString(), p1.toString());
        plugin.saveConfig();
    }

    public boolean areMarried(UUID p1, UUID p2) {
        String s = config.getString("marriages.married." + p1.toString(), null);
        if (s == null) return false;
        try {
            UUID other = UUID.fromString(s);
            return other.equals(p2);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
