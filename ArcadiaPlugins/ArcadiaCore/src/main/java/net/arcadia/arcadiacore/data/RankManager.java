package net.arcadia.arcadiacore.data;

import org.bukkit.entity.Player;

import java.util.UUID;

public class RankManager {

    private final DataManager dataManager;

    public RankManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public Rank getRank(UUID uuid, String name) {
        return dataManager.getOrCreate(uuid, name).getRank();
    }

    public void setRank(UUID uuid, String name, Rank rank) {
        dataManager.getOrCreate(uuid, name).setRank(rank);
    }

    public int getRankValue(UUID uuid, String name) {
        return getRank(uuid, name).getValue();
    }

    public boolean hasRankAtLeast(UUID uuid, String name, int value) {
        return getRankValue(uuid, name) >= value;
    }

    /**
     * Check if a player can use a command, considering:
     *  - explicit revokes (always deny)
     *  - explicit grants (always allow)
     *  - otherwise, rank value >= required
     */
    public boolean canUse(UUID uuid, String name, String cmdKey, int requiredRank) {
        ArcPlayerData pd = dataManager.getOrCreate(uuid, name);
        cmdKey = cmdKey.toLowerCase();

        if (pd.hasExplicitRevoke(cmdKey)) {
            return false;
        }
        if (pd.hasExplicitGrant(cmdKey)) {
            return true;
        }
        return pd.getRank().getValue() >= requiredRank;
    }

    public boolean canUse(Player player, String cmdKey, int requiredRank) {
        return canUse(player.getUniqueId(), player.getName(), cmdKey, requiredRank);
    }
}
