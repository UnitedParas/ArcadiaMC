package net.arcadia.arcadiacore.sail;

import net.arcadia.arcadiacore.ArcadiaCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

import static net.arcadia.arcadiacore.sail.SailStateManager.Mode;

public final class SailChanneler {

    public SailChanneler() {}

    public static void startChannel(Player p, Boat boat, SailStateManager.SailState s) {
        // Reset any previous
        cancelInternal(s);

        s.mode = Mode.CHANNELING;
        s.channelStartMs = System.currentTimeMillis();
        s.channelStartLoc = boat.getLocation().clone();
        s.boatId = boat.getUniqueId();

        p.sendMessage("§bChanneling... stay still for §f10§b seconds.");

        // 1-second repeating task
        s.channelTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                ArcadiaCore.getInstance(),
                new Runnable() {
                    int secondsLeft = 10;

                    @Override
                    public void run() {
                        if (s.mode != Mode.CHANNELING) {
                            cancelInternal(s);
                            return;
                        }

                        // Player must still be in the same boat
                        if (!(p.getVehicle() instanceof Boat current) || !current.getUniqueId().equals(s.boatId)) {
                            cancelChannel(p, s, "§cSailing canceled.");
                            return;
                        }

                        // Countdown messages at 10,5,3,2,1
                        if (secondsLeft == 10 || secondsLeft == 5 || secondsLeft == 3
                                || secondsLeft == 2 || secondsLeft == 1) {
                            p.sendMessage("§7" + secondsLeft + " second" + (secondsLeft == 1 ? "" : "s") + "...");
                        }

                        if (secondsLeft <= 1) {
                            // Complete channel
                            cancelInternal(s);
                            SailBoostUtil.enableBoostIfPossible(current, s, p);
                            s.mode = Mode.BOOSTING;
                            p.sendMessage("§aYou're ready to set sail!");
                            return;
                        }
                        secondsLeft--;
                    }
                },
                20L, 20L
        );
    }

    public static void cancelChannel(Player p, SailStateManager.SailState s, String msg) {
        cancelInternal(s);
        s.mode = Mode.IDLE;
        if (msg != null && !msg.isEmpty()) p.sendMessage(msg);
    }

    private static void cancelInternal(SailStateManager.SailState s) {
        if (s.channelTaskId != -1) {
            Bukkit.getScheduler().cancelTask(s.channelTaskId);
            s.channelTaskId = -1;
        }
        s.channelStartLoc = null;
        s.channelStartMs = 0L;
    }
}
