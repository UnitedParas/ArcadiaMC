package net.arcadia.arcadiacore.data;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ArcPlayerData {

    private final UUID uuid;
    private String name;
    private Rank rank;
    private int modcalls;
    private boolean modcallMuted;

    private final Set<String> grants;
    private final Set<String> revokes;
    private final List<String> notes;

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
        return grants.contains(cmdKey.toLowerCase());
    }

    public boolean hasExplicitRevoke(String cmdKey) {
        return revokes.contains(cmdKey.toLowerCase());
    }

    public void grantCommand(String cmdKey) {
        cmdKey = cmdKey.toLowerCase();
        revokes.remove(cmdKey);
        grants.add(cmdKey);
    }

    public void revokeCommand(String cmdKey) {
        cmdKey = cmdKey.toLowerCase();
        grants.remove(cmdKey);
        revokes.add(cmdKey);
    }
}
