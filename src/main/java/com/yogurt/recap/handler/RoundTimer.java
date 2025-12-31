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

    public void onRoundStart() {
        this.roundStartMillis = System.currentTimeMillis();
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
        // If we clearly left Zombies, stop the timer (so elapsed doesn't grow forever).
        if (!PlayerUtils.isInZombiesTitle()) {
            roundStartMillis = -1L;
        }
    }
}


