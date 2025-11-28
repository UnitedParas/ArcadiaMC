package net.arcadia.arcadiacore.sail;

import net.arcadia.arcadiacore.ArcadiaCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import static net.arcadia.arcadiacore.sail.SailStateManager.Mode;

public final class SailBoostUtil {

    private SailBoostUtil() {}

    // Tuning — adjust to taste
    private static final double TARGET_MAX_SPEED = 6.5;  // hard cap while boosting (horizontal)
    private static final double MIN_KICK_SPEED   = 1.00; // forward nudge from rest
    private static final double ACCEL_PER_TICK   = 0.35; // how quickly we ramp up

    public static void enableBoostIfPossible(Boat boat, SailStateManager.SailState s, Player p) {
        s.boosting = true;

        // Try native cap first (ignored on some forks, harmless if so)
        try {
            double currentMax = boat.getMaxSpeed();
            s.originalMaxSpeed = currentMax;
            boat.setMaxSpeed(currentMax * SailStateManager.BOOST_MULTIPLIER);
        } catch (Throwable ignored) {
            s.originalMaxSpeed = -1;
        }

        // 1-tick task that forces HORIZONTAL velocity; preserve Y so gravity works
        cancelBoostTaskIfAny(s);
        s.boostTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                ArcadiaCore.getInstance(),
                new Runnable() {
                    int hungerTicks = 0; // for hunger every 5s (100 ticks)

                    @Override
                    public void run() {
                        if (!s.boosting) {
                            cancelBoostTaskIfAny(s);
                            return;
                        }
                        if (!(p.getVehicle() instanceof Boat b)) {
                            cancelBoostTaskIfAny(s);
                            s.mode = Mode.IDLE;
                            return;
                        }
                        if (s.boatId == null || !s.boatId.equals(b.getUniqueId())) {
                            cancelBoostTaskIfAny(s);
                            s.mode = Mode.IDLE;
                            return;
                        }

                        // If not in water, end sailing so it can't be used on land/air
                        if (!b.isInWater()) {
                            disableBoostIfAny(b, s);
                            s.mode = Mode.IDLE;
                            p.sendMessage("§eSailing ended.");
                            return;
                        }

                        // Apply velocity boost
                        applyForcedBoost(b, SailStateManager.BOOST_MULTIPLIER);

                        // Hunger drain every 5 seconds
                        hungerTicks++;
                        if (hungerTicks >= 100) { // 100 ticks ~= 5 seconds
                            hungerTicks = 0;
                            int food = p.getFoodLevel();
                            if (food > 0) {
                                p.setFoodLevel(food - 1);
                            } else {
                                // no more food, stop sailing
                                disableBoostIfAny(b, s);
                                s.mode = Mode.IDLE;
                                p.sendMessage("§cYou are too exhausted to sail!");
                            }
                        }
                    }
                },
                1L, 1L
        );
    }

    public static void disableBoostIfAny(Boat boat, SailStateManager.SailState s) {
        if (!s.boosting && s.boostTaskId == -1) return;

        s.boosting = false;
        cancelBoostTaskIfAny(s);

        if (boat != null && s.originalMaxSpeed >= 0) {
            try {
                boat.setMaxSpeed(s.originalMaxSpeed);
            } catch (Throwable ignored) {}
        }
        s.originalMaxSpeed = -1;
    }

    private static void cancelBoostTaskIfAny(SailStateManager.SailState s) {
        if (s.boostTaskId != -1) {
            Bukkit.getScheduler().cancelTask(s.boostTaskId);
            s.boostTaskId = -1;
        }
    }

    /**
     * Force horizontal speed toward a target while preserving current Y so gravity and slopes behave normally.
     */
    private static void applyForcedBoost(Boat boat, double multiplier) {
        Vector v = boat.getVelocity();
        double speed = v.clone().setY(0).length(); // horizontal speed only

        // Forward direction (horizontal)
        Vector dir = boat.getLocation().getDirection();
        dir.setY(0);
        double len = dir.length();
        if (len < 1e-6) {
            // fallback to current horizontal motion
            Vector horiz = v.clone().setY(0);
            if (horiz.length() > 1e-6) dir = horiz.normalize();
            else return;
        } else {
            dir.multiply(1.0 / len);
        }

        // Target horizontal speed
        double scaled = Math.max(speed * multiplier, MIN_KICK_SPEED);
        double cappedTarget = Math.min(scaled, TARGET_MAX_SPEED);
        double newHoriz = Math.min(speed + ACCEL_PER_TICK, cappedTarget);
        if (newHoriz < MIN_KICK_SPEED) newHoriz = MIN_KICK_SPEED;

        // Preserve vertical component so gravity works
        double y = v.getY();
        Vector boosted = dir.multiply(newHoriz);
        boosted.setY(y);

        boat.setVelocity(boosted);
    }

    // Legacy name kept (now same as forced boost)
    public static void applyVelocityBoost(Boat boat, double multiplier) {
        applyForcedBoost(boat, multiplier);
    }
}
