package com.yogurt.recap;

import com.yogurt.recap.commands.RecapCommand;
import com.yogurt.recap.commands.RecapDebugCommand;
import com.yogurt.recap.config.ModConfig;
import com.yogurt.recap.features.killsgoldtracker.KillsGoldTracker;
import com.yogurt.recap.features.spawntimes.SpawnTimes;
import com.yogurt.recap.handler.RoundTimer;
import com.yogurt.recap.handler.ScoreboardManager;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(
        modid = YogurtRecapMod.MODID,
        name = YogurtRecapMod.NAME,
        version = YogurtRecapMod.VERSION,
        clientSideOnly = true
)
public class YogurtRecapMod {
    public static final String MODID = "yogurtrecap";
    public static final String NAME = "YogurtRecap";
    public static final String VERSION = "1.0.0";

    private static Logger LOGGER;

    private static final ScoreboardManager SCOREBOARD_MANAGER = new ScoreboardManager();
    private static final RoundTimer ROUND_TIMER = new RoundTimer();
    private static final SpawnTimes SPAWN_TIMES = new SpawnTimes();

    private static KillsGoldTracker killsGoldTracker;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        File cfgFile = new File(event.getModConfigurationDirectory(), MODID + ".cfg");
        ModConfig.load(new Configuration(cfgFile));
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(SCOREBOARD_MANAGER);
        MinecraftForge.EVENT_BUS.register(ROUND_TIMER);
        MinecraftForge.EVENT_BUS.register(SPAWN_TIMES);
        MinecraftForge.EVENT_BUS.register(killsGoldTracker = new KillsGoldTracker());
        
        // Register client-side commands
        ClientCommandHandler.instance.registerCommand(new RecapCommand());
        ClientCommandHandler.instance.registerCommand(new RecapDebugCommand());
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static ScoreboardManager getScoreboardManager() {
        return SCOREBOARD_MANAGER;
    }

    public static RoundTimer getRoundTimer() {
        return ROUND_TIMER;
    }

    public static SpawnTimes getSpawnTimes() {
        return SPAWN_TIMES;
    }
}


