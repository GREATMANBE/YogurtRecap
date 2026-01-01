package com.yogurt.recap.features.killsgoldtracker;

import com.yogurt.recap.YogurtRecapMod;
import com.yogurt.recap.config.ModConfig;
import com.yogurt.recap.utils.GameUtils;
import com.yogurt.recap.utils.PlayerUtils;
import com.yogurt.recap.utils.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Ported from ShowSpawnTime-2.1.1: tracks per-player kills (tablist objective) + gold (sidebar lines)
 * and reports per-round deltas (+ last-wave deltas when wave timing is available).
 */
public class KillsGoldTracker {

    private static final Minecraft minecraft = Minecraft.getMinecraft();
    private static final Pattern MC_FORMATTING_CODE = Pattern.compile("§.");

    // Track stats per player
    private static Map<String, PlayerStats> roundStartStats = new HashMap<>();     // round N start
    private static Map<String, PlayerStats> lastWaveStartStats = new HashMap<>(); // round N last-wave start

    private static int currentRound = 0;
    private static int currentWave = 0;
    private static final int REPORT_DELAY_TICKS = 40; // 2 second delay (40 ticks) - gives tablist time to populate

    private static PendingReport pendingReport = null;
    private static int snapshotDelayTicks = 0; // shared delay: captures (end of previous round) AND (start of current round)
    private static int lastRoundTitleSeen = 0;
    private static int titleMissingTicks = 0; // Count ticks where title is missing before resetting (prevents false resets during transitions)
    private static int round1TickCounter = 0; // Counter for Round 1 debug logging

    private static final class PendingReport {
        final int round;
        final Map<String, PlayerStats> roundStart;
        final Map<String, PlayerStats> lastWaveStart;

        private PendingReport(int round, Map<String, PlayerStats> roundStart, Map<String, PlayerStats> lastWaveStart) {
            this.round = round;
            this.roundStart = roundStart;
            this.lastWaveStart = lastWaveStart;
        }
    }

    // Called from MixinGuiIngame when round title is displayed
    public static void onRoundStart(int newRound) {
        if (!ModConfig.KILLS_GOLD_TRACKER_ENABLED) {
            return;
        }

        debug("onRoundStart newRound=" + newRound + " currentRound=" + currentRound
                + " snapshotDelayTicks=" + snapshotDelayTicks
                + " pendingReport=" + (pendingReport != null ? ("round=" + pendingReport.round + " startSize=" + pendingReport.roundStart.size()) : "null"));

        // Detect new game: if we see Round 1 but currentRound > 1, that means we started a new game
        // (you can't go backwards in rounds within a single game session).
        if (newRound == 1 && currentRound > 1) {
            reset();
            debug("Reset state because newRound=1 but currentRound=" + currentRound + " (new game detected)");
        }

        // If currentRound is 0, we're starting fresh (new game or just joined).
        // Reset everything to ensure clean state, especially if joining mid-game (Round 2+).
        if (currentRound == 0) {
            reset();
            debug("Reset state because currentRound was 0 (new game/joined mid-game)");
        }

        // If we're transitioning from an existing round to a new round, schedule report for the old round.
        if (currentRound > 0 && newRound != currentRound && pendingReport == null) {
            pendingReport = new PendingReport(
                    currentRound,
                    copyStatsMap(roundStartStats),      // start snapshot (captured at roundStart+delay)
                    copyStatsMap(lastWaveStartStats)    // last-wave snapshot (captured at last-wave start)
            );
            debug("Created pendingReport for round " + pendingReport.round
                    + " (roundStartStats.size=" + pendingReport.roundStart.size()
                    + ", lastWaveStartStats.size=" + pendingReport.lastWaveStart.size() + ")");
        }

        // Update current round immediately (so wave detection uses correct round),
        // but delay the "start snapshot" capture to (round title + REPORT_DELAY_TICKS)
        currentRound = newRound;
        currentWave = 0;
        lastWaveStartStats.clear();
        round1TickCounter = 0; // Reset debug counter on round change

        // Schedule a single delayed snapshot that will serve as:
        // - endStats for pendingReport (previous round)
        // - roundStartStats for the new currentRound (start snapshot at round+delay)
        snapshotDelayTicks = REPORT_DELAY_TICKS;
        lastRoundTitleSeen = newRound;
        debug("Scheduled snapshotDelayTicks=" + REPORT_DELAY_TICKS + " for round " + newRound);
    }

    // Data class to store player stats
    private static class PlayerStats {
        int kills;
        int gold;

        public PlayerStats(int kills, int gold) {
            this.kills = kills;
            this.gold = gold;
        }

        public PlayerStats copy() {
            return new PlayerStats(this.kills, this.gold);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        if (!ModConfig.KILLS_GOLD_TRACKER_ENABLED) {
            return;
        }

        // IMPORTANT: don't reset on brief "not in zombies" glitches during transitions.
        // Only reset when we are clearly out of Zombies (title gone for several ticks).
        if (!PlayerUtils.isInZombiesTitle()) {
            titleMissingTicks++;
            // Wait 10 ticks (0.5 seconds) before resetting to avoid false resets during brief title format changes
            if (titleMissingTicks >= 10) {
                // If we were tracking a round and the user left / game ended before the next round title,
                // flush a final report using a best-effort end snapshot.
                flushPendingReportOnExit();
                if (currentRound != 0 || snapshotDelayTicks != 0 || pendingReport != null) {
                    debug("Resetting because Zombies title is gone (likely left game).");
                }
                reset();
                return;
            }
            // Title is missing but we haven't waited long enough - continue processing but don't do wave detection
            return;
        } else {
            // Title is present - reset the missing counter
            titleMissingTicks = 0;
        }

        int wave = YogurtRecapMod.getSpawnTimes().getCurrentWave();
        
        // Debug: log wave value every 20 ticks for Round 1 to diagnose detection issues
        if (currentRound == 1) {
            round1TickCounter++;
            if (round1TickCounter % 20 == 0) { // Log every 20 ticks (~1 second)
                int elapsedMs = YogurtRecapMod.getRoundTimer().getElapsedMs();
                int[] roundTimes = GameUtils.getRoundTimes(1);
                debug("Round 1 wave check (tick=" + round1TickCounter + "): wave=" + wave + ", currentWave=" + currentWave 
                        + ", elapsedMs=" + elapsedMs + ", roundTimes.length=" + (roundTimes != null ? roundTimes.length : -1) 
                        + ", isLastWave=" + isLastWave(1, wave));
            }
        } else {
            round1TickCounter = 0; // Reset counter when not in Round 1
        }

        // Wave changed - check if it's the last wave
        // IMPORTANT: Only capture lastWaveStartStats when we transition TO the last wave,
        // not when we're already past it or at round start (wave == 0).
        if (wave != currentWave && wave > 0) {
            int oldWave = currentWave;
            currentWave = wave;
            debug("Wave changed: " + oldWave + " -> " + wave + " (round=" + currentRound + ", isLastWave=" + isLastWave(currentRound, wave) + ")");

            // Only capture if we just transitioned TO the last wave (not if we're already past it).
            // Also ensure we're not at round start (oldWave should be < wave, and wave should be the last wave).
            if (isLastWave(currentRound, wave) && oldWave < wave) {
                // Additional safety checks:
                // 1) Don't capture if roundStartStats hasn't been captured yet (still in delay period)
                // 2) Don't capture if elapsed time is suspiciously low (likely a timing bug)
                int elapsedMs = YogurtRecapMod.getRoundTimer().getElapsedMs();
                int[] roundTimes = GameUtils.getRoundTimes(currentRound);
                int minElapsedForLastWave = (roundTimes.length > 0) ? roundTimes[roundTimes.length - 1] * 1000 : 5000;
                
                if (!roundStartStats.isEmpty() && elapsedMs >= minElapsedForLastWave - 2000) {
                    // Allow 2 second tolerance before the expected last wave time
                    captureStats(lastWaveStartStats);
                    debug("Captured lastWaveStartStats at wave=" + wave + " for round=" + currentRound + " (elapsed=" + elapsedMs + "ms)");
                } else {
                    debug("Skipped lastWaveStartStats capture: roundStartStats.isEmpty=" + roundStartStats.isEmpty() 
                            + ", elapsedMs=" + elapsedMs + " (min=" + (minElapsedForLastWave - 2000) + "), wave=" + wave + ", round=" + currentRound);
                }
            }
        }

        // Count down the delay (if active)
        if (snapshotDelayTicks > 0) {
            snapshotDelayTicks--;
            if (snapshotDelayTicks == 0) {
                // Capture snapshot AFTER delay so late gold updates are included.
                Map<String, PlayerStats> snapshot = new HashMap<>();
                captureStats(snapshot);
                debug("Captured delayed snapshot for roundTitle=" + lastRoundTitleSeen + " snapshot.size=" + snapshot.size());

                // If we have a pending report, use this snapshot as the "endStats" for the previous round.
                if (pendingReport != null) {
                    if (pendingReport.roundStart.isEmpty()) {
                        debug("NOT reporting round " + pendingReport.round + " because roundStart snapshot is empty. "
                                + "Likely missed the start snapshot for that round.");
                    }
                    reportCombinedStats(pendingReport, snapshot);
                    pendingReport = null;
                }

                // Also use this same snapshot as the start snapshot for the current round (round title + delay).
                roundStartStats = snapshot;
            }
        }
    }

    private boolean isLastWave(int round, int wave) {
        int[] roundTimes = GameUtils.getRoundTimes(round);
        return roundTimes.length > 0 && wave == roundTimes.length;
    }

    private static void captureStats(Map<String, PlayerStats> storage) {
        storage.clear();

        // Try scoreboard-based capture first (matches original ShowSpawnTime flow)
        List<String> playerNames = getPlayerNamesFromScoreboard();

        for (String playerName : playerNames) {
            NetworkPlayerInfo playerInfo = getPlayerInfoFromTablist(playerName);
            if (playerInfo != null) {
                // IMPORTANT: Use canonical username (GameProfile.getName()) as the key, not the scoreboard name.
                // This ensures consistent keys between snapshots even if scoreboard/tablist show names differently.
                String canonicalUsername = playerInfo.getGameProfile() != null ? playerInfo.getGameProfile().getName() : null;
                if (canonicalUsername == null || canonicalUsername.isEmpty()) {
                    continue;
                }
                
                int kills = getKillsFromTablist(playerInfo);
                // Try getting gold using both the scoreboard name (for scoreboard-based lookup) and canonical username (fallback)
                int gold = getGoldFromScoreboard(playerName);
                if (gold == 0 && !playerName.equals(canonicalUsername)) {
                    // If scoreboard name lookup failed, try with canonical username
                    gold = getGoldFromScoreboard(canonicalUsername);
                }

                storage.put(canonicalUsername, new PlayerStats(kills, gold));
            }
        }

        // ALWAYS supplement with tablist enumeration to catch any players missed by scoreboard parsing.
        // This helps when:
        // - Scoreboard has gaps/empty lines that cause early break
        // - Scoreboard gold lines haven't appeared yet but tablist is ready
        // - Player names don't match exactly between scoreboard and tablist
        if (minecraft != null && minecraft.thePlayer != null && minecraft.thePlayer.sendQueue != null) {
            Collection<NetworkPlayerInfo> allPlayers = minecraft.thePlayer.sendQueue.getPlayerInfoMap();
            for (NetworkPlayerInfo info : allPlayers) {
                if (info.getGameProfile() == null) continue;
                String username = info.getGameProfile().getName();
                if (username == null || username.isEmpty()) continue;
                // Skip NPCs / non-players (Hypixel NPCs often have weird names)
                if (username.startsWith("!") || username.length() < 3 || username.length() > 16) continue;

                // Only add if not already captured from scoreboard (avoid duplicates)
                if (!storage.containsKey(username)) {
                    int kills = getKillsFromTablist(info);
                    int gold = getGoldFromScoreboard(username);

                    // Include player even if kills/gold are 0 (they might have 0 at round start)
                    // Only skip if we can't find them in tablist at all
                    storage.put(username, new PlayerStats(kills, gold));
                }
            }
        }
    }

    private static List<String> getPlayerNamesFromScoreboard() {
        List<String> playerNames = new ArrayList<>();

        for (int i = 6; i <= YogurtRecapMod.getScoreboardManager().getSize(); i++) {
            String content = YogurtRecapMod.getScoreboardManager().getContent(i);
            
            // Skip empty lines or lines starting with space, but continue scanning (don't break)
            // This allows us to find players even if there are gaps in the scoreboard
            if (content.startsWith(" ") || content.isEmpty()) {
                continue;
            }

            String colon = content.contains(":") ? ":" : (content.contains("：") ? "：" : "");
            // If no colon found, this line isn't a player gold line - skip it but continue
            if (colon.isEmpty()) {
                continue;
            }

            String playerName = stripFormatting(StringUtils.trim(content.split(colon)[0]));
            // Only add if we got a valid player name
            if (!playerName.isEmpty()) {
                playerNames.add(playerName);
            }
        }

        return playerNames;
    }

    private static int getGoldFromScoreboard(String playerName) {
        // Normalize the target player name (strip formatting for comparison)
        String targetName = stripFormatting(playerName);
        if (targetName.isEmpty()) {
            return 0;
        }

        for (int i = 6; i <= YogurtRecapMod.getScoreboardManager().getSize(); i++) {
            String content = YogurtRecapMod.getScoreboardManager().getContent(i);
            
            // Skip empty lines or lines starting with space
            if (content.startsWith(" ") || content.isEmpty()) {
                continue;
            }

            String colon = content.contains(":") ? ":" : (content.contains("：") ? "：" : "");
            if (colon.isEmpty()) {
                continue;
            }

            // Extract player name from this scoreboard line (same logic as getPlayerNamesFromScoreboard)
            String[] parts = content.split(colon);
            if (parts.length < 2) {
                continue;
            }
            String linePlayerName = stripFormatting(StringUtils.trim(parts[0]));

            // Match exactly (after stripping formatting) to avoid partial matches
            // (e.g., "Player" shouldn't match "Player12")
            if (linePlayerName.equals(targetName)) {
                // Gold is the number after the colon
                String goldPart = StringUtils.trim(parts[1]);

                // Remove all color codes (§ followed by any character)
                goldPart = goldPart.replaceAll("§.", "");

                // Remove commas and spaces
                goldPart = goldPart.replaceAll("[,\\s]", "");

                // Extract just the numbers
                try {
                    return Integer.parseInt(goldPart);
                } catch (NumberFormatException e) {
                    // If parsing fails, try to extract any sequence of digits
                    String numbers = goldPart.replaceAll("[^0-9]", "");
                    if (!numbers.isEmpty()) {
                        try {
                            return Integer.parseInt(numbers);
                        } catch (NumberFormatException ex) {
                            return 0;
                        }
                    }
                }
            }
        }

        return 0;
    }

    private static NetworkPlayerInfo getPlayerInfoFromTablist(String playerName) {
        if (minecraft == null || minecraft.thePlayer == null || minecraft.thePlayer.sendQueue == null) {
            return null;
        }

        String target = stripFormatting(playerName);
        if (target.isEmpty()) {
            return null;
        }

        Collection<NetworkPlayerInfo> playerInfos = minecraft.thePlayer.sendQueue.getPlayerInfoMap();

        // 1) Fast path: exact username match
        for (NetworkPlayerInfo info : playerInfos) {
            if (info.getGameProfile() != null) {
                String name = info.getGameProfile().getName();
                if (name != null && name.equals(target)) {
                    return info;
                }
            }
        }

        // 2) Fallback: match against stripped tablist display name (handles rank prefixes, icons, etc.)
        for (NetworkPlayerInfo info : playerInfos) {
            if (info.getGameProfile() == null) {
                continue;
            }
            if (info.getDisplayName() == null) {
                continue;
            }
            String formatted = info.getDisplayName().getFormattedText();
            String stripped = stripFormatting(formatted);
            if (stripped.contains(target)) {
                return info;
            }
        }

        return null;
    }

    private static int getKillsFromTablist(NetworkPlayerInfo playerInfo) {
        if (playerInfo == null) {
            return 0;
        }

        String username = playerInfo.getGameProfile() != null ? playerInfo.getGameProfile().getName() : "";

        // Primary method (correct for 1.8 tablist): the yellow number is usually the player-list scoreboard objective,
        // rendered separately from NetworkPlayerInfo#getDisplayName().
        try {
            if (!username.isEmpty() && minecraft != null && minecraft.theWorld != null) {
                Scoreboard scoreboard = minecraft.theWorld.getScoreboard();
                ScoreObjective playerListObjective = scoreboard.getObjectiveInDisplaySlot(0); // tablist objective
                if (playerListObjective != null) {
                    Score score = scoreboard.getValueFromObjective(username, playerListObjective);
                    if (score != null) {
                        return score.getScorePoints();
                    }
                }
            }
        } catch (Exception ignored) {
            // fall back to parsing formatted name below
        }

        // Use formatted tablist text so we can detect the yellow-colored number segment.
        String formatted = "";
        if (playerInfo.getDisplayName() != null) {
            formatted = playerInfo.getDisplayName().getFormattedText();
        }
        if (formatted == null || formatted.isEmpty()) {
            // If no formatted display name is available, we can't reliably parse the colored segment.
            return 0;
        }

        // Find where the username ends in the stripped text; kills appear after the nickname in tablist.
        String stripped = stripFormatting(formatted);
        int nameIndex = username.isEmpty() ? -1 : stripped.indexOf(username);
        int startStrippedPos = (nameIndex >= 0) ? (nameIndex + username.length()) : 0;

        // Walk the formatted text, tracking current color, and extract the first yellow-colored number after the name.
        StringBuilder digits = new StringBuilder();
        char currentColor = 0;
        int strippedPos = 0;
        boolean collecting = false;

        for (int i = 0; i < formatted.length(); i++) {
            char c = formatted.charAt(i);
            if (c == '§' && i + 1 < formatted.length()) {
                currentColor = Character.toLowerCase(formatted.charAt(i + 1));
                i++; // skip color code char
                continue;
            }

            // This character is visible; advance stripped position.
            boolean afterName = strippedPos >= startStrippedPos;

            // Hypixel uses '§e' (yellow) commonly; sometimes '§6' (gold) is used for yellow-ish numbers.
            boolean isYellow = currentColor == 'e' || currentColor == '6';
            boolean isDigitOrComma = (c >= '0' && c <= '9') || c == ',';

            if (!collecting) {
                if (afterName && isYellow && isDigitOrComma) {
                    collecting = true;
                    digits.append(c);
                }
            } else {
                // Stop if color changes away from yellow or the sequence ends.
                if (!isYellow || !isDigitOrComma) {
                    break;
                }
                digits.append(c);
            }

            strippedPos++;
        }

        if (digits.length() > 0) {
            String number = digits.toString().replace(",", "");
            try {
                return Integer.parseInt(number);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }

        // Fallback: no colored segment found.
        return 0;
    }

    private static String stripFormatting(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return MC_FORMATTING_CODE.matcher(s).replaceAll("").trim();
    }

    private static void reportCombinedStats(PendingReport report, Map<String, PlayerStats> endStats) {
        if (report == null || report.roundStart.isEmpty() || endStats == null || endStats.isEmpty()) {
            debug("reportCombinedStats skipped (report null/empty start or endStats empty). "
                    + "report=" + (report == null ? "null" : ("round=" + report.round + " startSize=" + report.roundStart.size()))
                    + " endStatsSize=" + (endStats == null ? "null" : endStats.size()));
            return;
        }

        // Calculate differences for entire round (endStats - roundStart)
        List<PlayerStatsDiff> roundDiffs = new ArrayList<>();
        for (Map.Entry<String, PlayerStats> entry : endStats.entrySet()) {
            String playerName = entry.getKey();
            PlayerStats end = entry.getValue();
            PlayerStats start = report.roundStart.get(playerName);
            if (start != null) {
                roundDiffs.add(new PlayerStatsDiff(playerName, end.kills - start.kills, end.gold - start.gold));
            }
        }

        // Calculate differences for last wave only (endStats - lastWaveStart)
        List<PlayerStatsDiff> lastWaveDiffs = new ArrayList<>();
        if (!report.lastWaveStart.isEmpty()) {
            for (Map.Entry<String, PlayerStats> entry : endStats.entrySet()) {
                String playerName = entry.getKey();
                PlayerStats end = entry.getValue();
                PlayerStats start = report.lastWaveStart.get(playerName);
                if (start != null) {
                    lastWaveDiffs.add(new PlayerStatsDiff(playerName, end.kills - start.kills, end.gold - start.gold));
                }
            }
        }

        // Sort by kills (descending)
        roundDiffs.sort((a, b) -> Integer.compare(b.kills, a.kills));
        lastWaveDiffs.sort((a, b) -> Integer.compare(b.kills, a.kills));

        // Build single multi-line message
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(EnumChatFormatting.GOLD).append(EnumChatFormatting.STRIKETHROUGH).append("━━━━━━━━━━━━━━━━━━━━━\n");
        messageBuilder.append(EnumChatFormatting.YELLOW).append(EnumChatFormatting.BOLD).append("Round ").append(report.round).append(" Complete!\n");
        messageBuilder.append("\n");

        // Show full round stats
        messageBuilder.append(EnumChatFormatting.AQUA).append("Full Round:\n");
        for (PlayerStatsDiff diff : roundDiffs) {
            messageBuilder.append(EnumChatFormatting.WHITE).append("  ").append(diff.playerName).append(": ");
            messageBuilder.append(EnumChatFormatting.RED).append(diff.kills).append(" kills ");
            messageBuilder.append(EnumChatFormatting.GRAY).append("| ");
            messageBuilder.append(EnumChatFormatting.GOLD).append("+").append(diff.gold).append(" gold\n");
        }

        // Show last wave stats if available
        if (!lastWaveDiffs.isEmpty()) {
            messageBuilder.append("\n");
            messageBuilder.append(EnumChatFormatting.AQUA).append("Last Wave:\n");
            for (PlayerStatsDiff diff : lastWaveDiffs) {
                messageBuilder.append(EnumChatFormatting.WHITE).append("  ").append(diff.playerName).append(": ");
                messageBuilder.append(EnumChatFormatting.RED).append(diff.kills).append(" kills ");
                messageBuilder.append(EnumChatFormatting.GRAY).append("| ");
                messageBuilder.append(EnumChatFormatting.GOLD).append("+").append(diff.gold).append(" gold\n");
            }
        }

        messageBuilder.append(EnumChatFormatting.GOLD).append(EnumChatFormatting.STRIKETHROUGH).append("━━━━━━━━━━━━━━━━━━━━━");

        // Send as single message
        PlayerUtils.sendMessage(messageBuilder.toString());
    }

    private static Map<String, PlayerStats> copyStatsMap(Map<String, PlayerStats> input) {
        Map<String, PlayerStats> copy = new HashMap<>();
        if (input == null) {
            return copy;
        }
        for (Map.Entry<String, PlayerStats> e : input.entrySet()) {
            PlayerStats v = e.getValue();
            if (v != null) {
                copy.put(e.getKey(), v.copy());
            }
        }
        return copy;
    }

    private static void reset() {
        roundStartStats.clear();
        lastWaveStartStats.clear();
        currentRound = 0;
        currentWave = 0;
        snapshotDelayTicks = 0;
        pendingReport = null;
        lastRoundTitleSeen = 0;
        titleMissingTicks = 0;
    }

    private static void flushPendingReportOnExit() {
        try {
            if (!ModConfig.KILLS_GOLD_TRACKER_ENABLED) {
                return;
            }

            // If we don't have a start snapshot, we can't compute diffs reliably.
            if (currentRound <= 0 || roundStartStats.isEmpty()) {
                return;
            }

            Map<String, PlayerStats> endSnapshot = new HashMap<>();
            captureStats(endSnapshot);
            if (endSnapshot.isEmpty()) {
                return;
            }

            if (pendingReport != null) {
                reportCombinedStats(pendingReport, endSnapshot);
                pendingReport = null;
                return;
            }

            // No pendingReport means we never saw the next round title, so report currentRound using current start snapshot.
            PendingReport report = new PendingReport(
                    currentRound,
                    copyStatsMap(roundStartStats),
                    copyStatsMap(lastWaveStartStats)
            );
            reportCombinedStats(report, endSnapshot);
        } catch (Exception ignored) {
            // Never crash the client if something goes wrong during exit flush.
        }
    }

    private static void debug(String msg) {
        if (!ModConfig.KILLS_GOLD_TRACKER_DEBUG) {
            return;
        }
        if (minecraft == null || minecraft.thePlayer == null) {
            return;
        }
        minecraft.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_GRAY + "[KGT DEBUG] " + msg));
    }

    // Helper class for player stats difference
    private static class PlayerStatsDiff {
        String playerName;
        int kills;
        int gold;

        public PlayerStatsDiff(String playerName, int kills, int gold) {
            this.playerName = playerName;
            this.kills = kills;
            this.gold = gold;
        }
    }
}


