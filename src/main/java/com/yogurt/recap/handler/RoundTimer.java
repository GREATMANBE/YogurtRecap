package com.yogurt.recap.handler;

import com.yogurt.recap.utils.PlayerUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Simple wall-clock timer used to estimate "current wave" based on elapsed ms since the round title appeared.
 * This is a lightweight replacement for ShowSpawnTime's scheduled 10ms tick counter.
 */
public class RoundTimer {
    private long roundStartMillis = -1L;
    private int titleMissingTicks = 0; // Tolerance before resetting (prevents false resets during brief title changes)

    public void onRoundStart() {
        this.roundStartMillis = System.currentTimeMillis();
        this.titleMissingTicks = 0; // Reset tolerance counter on new round
    }

    public int getElapsedMs() {
        if (roundStartMillis <= 0L) {
            return 0;
        }
        long delta = System.currentTimeMillis() - roundStartMillis;
        if (delta <= 0L) {
            return 0;
        }
        if (delta > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) delta;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        // IMPORTANT: Don't reset immediately on brief "not in zombies" glitches during transitions.
        // Wait 10 ticks (same as KillsGoldTracker) before resetting to avoid false resets.
        if (!PlayerUtils.isInZombiesTitle()) {
            titleMissingTicks++;
            if (titleMissingTicks >= 10) {
                roundStartMillis = -1L;
                titleMissingTicks = 0;
            }
        } else {
            titleMissingTicks = 0; // Title is present - reset the missing counter
        }
    }
}


