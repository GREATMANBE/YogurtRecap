package com.yogurt.recap.mixins;

import com.yogurt.recap.YogurtRecapMod;
import com.yogurt.recap.features.killsgoldtracker.KillsGoldTracker;
import com.yogurt.recap.utils.StringUtils;
import net.minecraft.client.gui.GuiIngame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Minimal hook: when Hypixel displays the round title, notify the tracker and reset the round timer.
 *
 * Ported conceptually from ShowSpawnTime's {@code MixinGuiIngame#displayTitle} injection.
 */
@Mixin(GuiIngame.class)
public abstract class MixinGuiIngame {

    /**
     * A small set of common "Round" tokens across languages.
     * This is intentionally lightweight (no i18n tables), but much more robust than English-only matching.
     */
    private static final String[] ROUND_TOKENS = new String[]{
            "ROUND",     // EN
            "回合",       // ZH
            "라운드",      // KO
            "ラウンド",     // JA
            "РАУНД",      // RU
            "RUNDE",     // DE/DA/NO
            "RONDE",     // FR/NL
            "RODADA",    // PT
            "RONDA"      // ES
    };

    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d+.*");

    @Inject(method = "displayTitle", at = @At(value = "RETURN"))
    private void yogurtrecap$displayTitle(String title, String subtitle, int fadeIn, int displayTime, int fadeOut, CallbackInfo ci) {
        String t = StringUtils.trim(title);
        if (t.isEmpty()) {
            return;
        }

        // Normalize for matching
        String upper = t.toUpperCase(Locale.ROOT);

        int round = 0;
        boolean isRoundTitle = false;
        if (HAS_DIGIT.matcher(upper).matches()) {
            for (String token : ROUND_TOKENS) {
                if (upper.contains(token)) {
                    isRoundTitle = true;
                    break;
                }
            }
        }

        // End-of-game titles: treat as round=0 to force a final report for the previous round.
        // (Original ShowSpawnTime also triggers logic on win/gameover titles.)
        boolean isEndTitle = upper.contains("YOU WIN") || upper.contains("GAME OVER");

        if (isRoundTitle) {
            round = StringUtils.getNumberInString(upper);
        } else if (isEndTitle) {
            round = 0;
        } else {
            return;
        }

        // Reset timer and update cached round for wave estimation.
        // Only start timing when an actual round starts.
        if (round > 0) {
            YogurtRecapMod.getRoundTimer().onRoundStart();
            YogurtRecapMod.getSpawnTimes().setCurrentRound(round);
        }

        // Inform tracker (schedules report for prior round + captures round start snapshot after a delay).
        KillsGoldTracker.onRoundStart(round);
    }
}


