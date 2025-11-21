package net.arcadia.arcadiacore.data;

public enum Rank {
    FULL(10),
    ADMIN(8),
    STAFF(5),
    TRUSTED(3),
    DONOR(3),
    PLAYER(2),
    GUEST(1),
    BANISHED(0);

    private final int value;

    Rank(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Rank fromString(String s) {
        if (s == null) return PLAYER;
        try {
            return Rank.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return PLAYER;
        }
    }
}
