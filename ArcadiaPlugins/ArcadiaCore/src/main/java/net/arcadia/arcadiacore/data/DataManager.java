package net.arcadia.arcadiacore.data;

import net.arcadia.arcadiacore.ArcadiaCore;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataManager {

    private final ArcadiaCore plugin;
    private final Map<UUID, ArcPlayerData> dataMap = new HashMap<>();

    public DataManager(ArcadiaCore plugin) {
        this.plugin = plugin;
        loadAll();
    }

    public ArcPlayerData getOrCreate(UUID uuid, String name) {
        ArcPlayerData data = dataMap.get(uuid);
        if (data == null) {
            data = new ArcPlayerData(uuid, name);
            dataMap.put(uuid, data);
        } else {
            data.setName(name);
        }
        return data;
    }

    public ArcPlayerData get(UUID uuid) {
        return dataMap.get(uuid);
    }

    public Collection<ArcPlayerData> getAll() {
        return dataMap.values();
    }

    // ========== SAVE / LOAD ==========

    public void saveAll() {
        File dataRoot = new File(plugin.getDataFolder(), "data");

        // Clean all rank folders (so moving ranks doesn't leave old files)
        for (Rank rank : Rank.values()) {
            File dir = new File(dataRoot, rank.name());
            if (dir.exists()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile()) {
                            //noinspection ResultOfMethodCallIgnored
                            f.delete();
                        }
                    }
                }
            } else {
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            }
        }

        // Now save each player into their rank folder, filename = username.yml
        for (ArcPlayerData pd : dataMap.values()) {
            Rank rank = pd.getRank();
            File dir = new File(dataRoot, rank.name());
            if (!dir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            }

            String safeName = sanitizeFileName(pd.getName());
            if (safeName.isEmpty()) {
                safeName = pd.getUuid().toString();
            }

            File file = new File(dir, safeName + ".yml");
            YamlConfiguration cfg = new YamlConfiguration();

            cfg.set("name", pd.getName());
            cfg.set("uuid", pd.getUuid().toString());
            cfg.set("rank", pd.getRank().name());
            cfg.set("modcalls", pd.getModcalls());
            cfg.set("modcallMuted", pd.isModcallMuted());
            cfg.set("grants", new ArrayList<>(pd.getGrants()));
            cfg.set("revokes", new ArrayList<>(pd.getRevokes()));
            cfg.set("notes", new ArrayList<>(pd.getNotes()));

            try {
                cfg.save(file);
            } catch (IOException e) {
                plugin.getLogger().warning("[ArcadiaCore] Failed to save player file " +
                        file.getAbsolutePath() + ": " + e.getMessage());
            }
        }
    }

    private void loadAll() {
        File dataRoot = new File(plugin.getDataFolder(), "data");
        if (!dataRoot.exists()) {
            return; // first run, nothing yet
        }

        for (Rank rank : Rank.values()) {
            File dir = new File(dataRoot, rank.name());
            if (!dir.exists() || !dir.isDirectory()) continue;

            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".yml"));
            if (files == null) continue;

            for (File file : files) {
                try {
                    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

                    String uuidStr = cfg.getString("uuid");
                    if (uuidStr == null || uuidStr.isEmpty()) {
                        plugin.getLogger().warning("[ArcadiaCore] Missing uuid in " + file.getName());
                        continue;
                    }

                    UUID uuid = UUID.fromString(uuidStr);
                    String name = cfg.getString("name", "Unknown");
                    Rank storedRank = Rank.fromString(cfg.getString("rank", rank.name()));

                    int modcalls = cfg.getInt("modcalls", 0);
                    boolean modcallMuted = cfg.getBoolean("modcallMuted", false);
                    List<String> grants = cfg.getStringList("grants");
                    List<String> revokes = cfg.getStringList("revokes");
                    List<String> notes = cfg.getStringList("notes");

                    ArcPlayerData pd = new ArcPlayerData(uuid, name);
                    pd.setRank(storedRank);
                    pd.setModcalls(modcalls);
                    pd.setModcallMuted(modcallMuted);
                    grants.forEach(pd::grantCommand);
                    revokes.forEach(pd::revokeCommand);
                    notes.forEach(pd::addNote);

                    dataMap.put(uuid, pd);
                } catch (Exception ex) {
                    plugin.getLogger().warning("[ArcadiaCore] Failed to load player file "
                            + file.getAbsolutePath() + ": " + ex.getMessage());
                }
            }
        }
    }

    private String sanitizeFileName(String input) {
        if (input == null) return "";
        // Only allow letters, digits, underscore and dash
        return input.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }
}
