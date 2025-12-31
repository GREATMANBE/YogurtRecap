package com.yogurt.recap.config;

import net.minecraftforge.common.config.Configuration;

public final class ModConfig {
    private ModConfig() {}

    public static boolean KILLS_GOLD_TRACKER_ENABLED = true;
    public static boolean KILLS_GOLD_TRACKER_DEBUG = false;

    public static void load(Configuration cfg) {
        try {
            cfg.load();
            KILLS_GOLD_TRACKER_ENABLED = cfg.get(Configuration.CATEGORY_GENERAL,
                    "Kills and Gold Tracking", true,
                    "Track and display kills and gold earned by each player at the end of each round and after the last wave.")
                    .getBoolean(true);
            KILLS_GOLD_TRACKER_DEBUG = cfg.get(Configuration.CATEGORY_GENERAL,
                    "Kills and Gold Tracking Debug", false,
                    "Debug logging for Kills and Gold Tracking (prints debug lines in chat).")
                    .getBoolean(false);
        } finally {
            if (cfg.hasChanged()) {
                cfg.save();
            }
        }
    }
}


