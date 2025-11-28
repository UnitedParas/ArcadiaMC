package net.arcadia.arcadiacore.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-player persistent data for ArcadiaCore.
 */
public class ArcPlayerData {

    private final UUID uuid;
    private String name;
    private Rank rank;
    private int modcalls;
    private boolean modcallMuted;

    private final Set<String> grants;
    private final Set<String> revokes;
    private final List<String> notes;

    // currencyName(lowercase) -> amount
    private final Map<String, Double> wallet = new HashMap<>();

    public ArcPlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.rank = Rank.PLAYER; // default rank
        this.modcalls = 0;
        this.modcallMuted = false;
        this.grants = new HashSet<>();
        this.revokes = new HashSet<>();
        this.notes = new LinkedList<>();
    }

    public UUID getUuid() { return uuid; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public Rank getRank() { return rank; }

    public void setRank(Rank rank) { this.rank = rank; }

    public int getModcalls() { return modcalls; }

    public void setModcalls(int modcalls) { this.modcalls = modcalls; }

    public void addModcalls(int delta) { this.modcalls += delta; }

    public boolean isModcallMuted() { return modcallMuted; }

    public void setModcallMuted(boolean modcallMuted) { this.modcallMuted = modcallMuted; }

    public Set<String> getGrants() { return grants; }

    public Set<String> getRevokes() { return revokes; }

    public List<String> getNotes() { return notes; }

    public void addNote(String note) { this.notes.add(note); }

    public boolean hasExplicitGrant(String cmdKey) {
        return grants.contains(cmdKey.toLowerCase(Locale.ROOT));
    }

    public boolean hasExplicitRevoke(String cmdKey) {
        return revokes.contains(cmdKey.toLowerCase(Locale.ROOT));
    }

    public void grantCommand(String cmdKey) {
        cmdKey = cmdKey.toLowerCase(Locale.ROOT);
        revokes.remove(cmdKey);
        grants.add(cmdKey);
    }

    public void revokeCommand(String cmdKey) {
        cmdKey = cmdKey.toLowerCase(Locale.ROOT);
        grants.remove(cmdKey);
        revokes.add(cmdKey);
    }

    // ---- Convenience wrappers for older code ----

    public boolean isCommandGranted(String cmdKey) {
        return hasExplicitGrant(cmdKey);
    }

    public boolean isCommandRevoked(String cmdKey) {
        return hasExplicitRevoke(cmdKey);
    }

    // ==== WALLET / CURRENCIES ====

    /** Internal map (lowercase currency -> amount). */
    public Map<String, Double> getWallet() {
        return wallet;
    }

    /** Alias to keep your WalletCommand happy. */
    public Map<String, Double> getBalances() {
        return wallet;
    }

    public double getBalance(String currency) {
        if (currency == null) return 0.0;
        return wallet.getOrDefault(currency.toLowerCase(Locale.ROOT), 0.0);
    }

    public void setBalance(String currency, double amount) {
        if (currency == null) return;
        String key = currency.toLowerCase(Locale.ROOT);
        if (amount <= 0.0) {
            wallet.remove(key);
        } else {
            wallet.put(key, amount);
        }
    }

    public void addBalance(String currency, double delta) {
        if (currency == null || delta == 0.0) return;
        String key = currency.toLowerCase(Locale.ROOT);
        double old = wallet.getOrDefault(key, 0.0);
        double now = old + delta;
        if (now <= 0.0) {
            wallet.remove(key);
        } else {
            wallet.put(key, now);
        }
    }
}
