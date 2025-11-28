package net.arcadia.arcadiacore.sail;

import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

import static net.arcadia.arcadiacore.sail.SailStateManager.Mode;

public class SailListener implements Listener {

    private final SailStateManager state;

    public SailListener(SailStateManager state) {
        this.state = state;
    }

    @EventHandler
    public void onEnter(VehicleEnterEvent e) {
        if (!(e.getVehicle() instanceof Boat boat)) return;
        if (!(e.getEntered() instanceof Player p)) return;
        SailStateManager.SailState s = state.get(p.getUniqueId());
        s.boatId = boat.getUniqueId();
    }

    @EventHandler
    public void onExit(VehicleExitEvent e) {
        if (!(e.getVehicle() instanceof Boat boat)) return;
        if (!(e.getExited() instanceof Player p)) return;
        SailStateManager.SailState s = state.get(p.getUniqueId());

        if (s.mode == Mode.CHANNELING) {
            SailChanneler.cancelChannel(p, s, "§eSailing canceled.");
        } else if (s.mode == Mode.BOOSTING) {
            SailBoostUtil.disableBoostIfAny(boat, s);
            s.mode = Mode.IDLE;
            p.sendMessage("§eSailing ended.");
        }
    }

    @EventHandler
    public void onVehicleDestroyed(VehicleDestroyEvent e) {
        if (!(e.getVehicle() instanceof Boat boat)) return;
        for (var entry : state.all()) {
            var s = entry.getValue();
            if (s.boatId != null && s.boatId.equals(boat.getUniqueId())) {
                s.mode = Mode.IDLE;
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        state.clear(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent e) {
        if (!(e.getVehicle() instanceof Boat boat)) return;

        var from = e.getFrom();
        var to   = e.getTo();
        double distSq = 0.0;
        if (from != null && to != null) {
            double dx = to.getX() - from.getX();
            double dy = to.getY() - from.getY();
            double dz = to.getZ() - from.getZ();
            distSq = dx*dx + dy*dy + dz*dz;
        }

        for (Entity passenger : boat.getPassengers()) {
            if (!(passenger instanceof Player p)) continue;
            var s = state.get(p.getUniqueId());

            // If channeling and you leave water, cancel
            if (s.mode == Mode.CHANNELING) {
                if (!boat.isInWater()) {
                    SailChanneler.cancelChannel(p, s, "§cSailing canceled! You left the water.");
                    continue;
                }
                if (distSq > SailStateManager.STILL_THRESH_SQ) {
                    SailChanneler.cancelChannel(p, s, "§cSailing canceled! You moved.");
                }
            }
            // boosting is handled in SailBoostUtil
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p) {
            var s = state.get(p.getUniqueId());
            if (s.mode == Mode.CHANNELING) {
                SailChanneler.cancelChannel(p, s, "§cSailing canceled! You took damage.");
            } else if (s.mode == Mode.BOOSTING) {
                // stop sailing if hurt while boosting
                if (p.getVehicle() instanceof Boat boat) {
                    SailBoostUtil.disableBoostIfAny(boat, s);
                } else {
                    SailBoostUtil.disableBoostIfAny(null, s);
                }
                s.mode = Mode.IDLE;
                p.sendMessage("§cSailing canceled! You took damage.");
            }
        } else if (e.getEntity() instanceof Boat boat) {
            for (Entity passenger : boat.getPassengers()) {
                if (passenger instanceof Player p) {
                    var s = state.get(p.getUniqueId());
                    if (s.mode == Mode.CHANNELING) {
                        SailChanneler.cancelChannel(p, s, "§cSailing canceled! Your boat took damage.");
                    } else if (s.mode == Mode.BOOSTING) {
                        SailBoostUtil.disableBoostIfAny(boat, s);
                        s.mode = Mode.IDLE;
                        p.sendMessage("§cSailing canceled! Your boat took damage.");
                    }
                }
            }
        }
    }
}
