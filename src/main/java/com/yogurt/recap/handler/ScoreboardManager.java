package com.yogurt.recap.handler;

import com.yogurt.recap.utils.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Lightweight scoreboard snapshotter (sidebar slot 1), adapted from ShowSpawnTime.
 */
public class ScoreboardManager {
    private String title = "";
    private List<String> content = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public List<String> getContents() {
        return content;
    }

    public int getSize() {
        return content.size();
    }

    public String getContent(int row) {
        if (row > this.getSize() || row < 1) {
            return "";
        }
        return content.get(row - 1);
    }

    public void clear() {
        this.title = "";
        this.content = new ArrayList<>();
    }

    public void updateScoreboardContent() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null) {
            return;
        }
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(1);
        if (sidebarObjective == null) {
            this.title = "";
            this.content = new ArrayList<>();
            return;
        }

        this.title = StringUtils.trim(sidebarObjective.getDisplayName());

        List<String> scoreboardLines = new ArrayList<>();
        Collection<Score> scores = scoreboard.getSortedScores(sidebarObjective);
        List<Score> filteredScores = scores.stream()
                .filter(s -> s.getPlayerName() != null && !s.getPlayerName().startsWith("#"))
                .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
        if (filteredScores.isEmpty()) {
            this.content = scoreboardLines;
            return;
        }

        Collections.reverse(filteredScores);
        for (Score line : filteredScores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(line.getPlayerName());
            String scoreboardLine = ScorePlayerTeam.formatPlayerName(team, line.getPlayerName()).trim();
            scoreboardLines.add(StringUtils.trim(scoreboardLine));
        }
        this.content = scoreboardLines;
    }

    private int tick = 0;

    @SubscribeEvent
    public void onUpdate(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || mc.isSingleplayer()) {
            return;
        }
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        EntityPlayerSP p = mc.thePlayer;
        if (p == null) {
            return;
        }

        tick++;
        if (tick != 0 && tick % 5 == 0) {
            updateScoreboardContent();
            tick = 0;
        }
    }
}


