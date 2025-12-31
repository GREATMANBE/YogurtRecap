package com.yogurt.recap.features.spawntimes;

import com.yogurt.recap.YogurtRecapMod;
import com.yogurt.recap.utils.GameUtils;
import com.yogurt.recap.utils.JavaUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Minimal subset of ShowSpawnTime's SpawnTimes: only what's needed for KillsGoldTracker
 * (estimating current wave by elapsed time since round start + per-map round timing tables).
 */
public class SpawnTimes {
    public int currentRound = 0;
    private int currentWave = 0;
    private int[] roundTimes = new int[0];

    public void setCurrentRound(int round) {
        this.currentRound = round;
        this.roundTimes = GameUtils.getRoundTimes(currentRound);
        this.currentWave = 0;
    }

    public int getCurrentWave() {
        if (currentRound <= 0) {
            return 0;
        }
        if (roundTimes.length == 0) {
            roundTimes = GameUtils.getRoundTimes(currentRound);
        }
        if (roundTimes.length == 0) {
            return 0; // No timing data for this round
        }
        int[] roundTicks = roundTimes.clone();
        for (int i = 0; i < roundTicks.length; i++) {
            roundTicks[i] *= 1000;
        }
        int elapsedMs = YogurtRecapMod.getRoundTimer().getElapsedMs();
        // Safety: if elapsed time is negative or suspiciously large, return 0
        if (elapsedMs < 0 || elapsedMs > 300000) { // 5 minutes max
            return 0;
        }
        // Match ShowSpawnTime behavior: wave is the insert position (0..roundTimes.length).
        // When it reaches roundTimes.length, the "last wave has spawned" condition becomes true.
        currentWave = JavaUtils.findInsertPosition(roundTicks, elapsedMs);
        // Clamp to valid range
        if (currentWave < 0) currentWave = 0;
        if (currentWave > roundTimes.length) currentWave = roundTimes.length;
        return currentWave;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        // Keep cached wave reasonably fresh, but don't do work if we haven't started.
        if (currentRound > 0) {
            getCurrentWave();
        }
    }
}


