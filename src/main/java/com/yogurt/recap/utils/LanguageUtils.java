package com.yogurt.recap.utils;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

/**
 * Minimal language/map utility:
 * - Detect whether the sidebar title is Hypixel Zombies.
 * - Detect which Zombies map is being played (via a block check, as in ShowSpawnTime).
 * - Provide per-map timing tables used to infer wave/last-wave boundaries.
 */
public final class LanguageUtils {
    private LanguageUtils() {}

    public static boolean isZombiesTitle(String title) {
        title = StringUtils.trim(title);
        return title.contains("ZOMBIES") || title.contains("僵尸末日") || title.contains("殭屍末日");
    }

    public static ZombiesMap getMap() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return ZombiesMap.NULL;
        }
        World world = mc.theWorld;
        if (world == null) {
            return ZombiesMap.NULL;
        }

        // Same heuristic as ShowSpawnTime: a map-unique block at a fixed coordinate.
        BlockPos blockPos = new BlockPos(0, 72, 12);
        IBlockState blockState;
        try {
            blockState = world.getBlockState(blockPos);
        } catch (Exception ignored) {
            return ZombiesMap.NULL;
        }
        if (blockState == null) {
            return ZombiesMap.NULL;
        }
        Block block = blockState.getBlock();
        if (block == null) {
            return ZombiesMap.NULL;
        }
        String blockName = block.getUnlocalizedName();
        if (blockName == null) {
            return ZombiesMap.NULL;
        }

        switch (blockName) {
            case "tile.air":
                return ZombiesMap.THE_LAB;
            case "tile.woolCarpet":
                return ZombiesMap.ALIEN_ARCADIUM;
            case "tile.stonebricksmooth":
                return ZombiesMap.BAD_BLOOD;
            case "tile.cloth":
                return ZombiesMap.DEAD_END;
            case "tile.clayHardenedStained":
                return ZombiesMap.PRISON;
            default:
                return ZombiesMap.NULL;
        }
    }

    public enum ZombiesMap {
        NULL(new int[][]{}, 0),
        PRISON(new int[][]{
                {10, 20}, {10, 20, 30}, {10, 17, 24, 31}, {10, 17, 24, 31}, {10, 20, 30}, {10, 20, 30}, {10, 20, 30},
                {10, 25, 40}, {10, 25, 35}, {10, 25, 45}, {10, 25, 40}, {10, 25, 37}, {10, 22, 34}, {10, 25, 37},
                {10, 25, 40}, {10, 22, 37}, {10, 22, 42}, {10, 25, 45}, {10, 25, 45}, {10, 25, 40}, {10, 20, 35, 55, 75},
                {10, 25, 40}, {10, 30, 50}, {10, 30, 50}, {10, 25, 45}, {10, 30, 50}, {10, 25, 45}, {10, 30, 50}, {10, 30, 55}, {10}
        }, 30),
        THE_LAB(new int[][]{
                {10, 22}, {10, 22}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34},
                {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34},
                {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34},
                {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34},
                {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}
        }, 40),
        DEAD_END(new int[][]{
                {10, 20}, {10, 20}, {10, 20, 35}, {10, 20, 35}, {10, 22, 37}, {10, 22, 44}, {10, 25, 47}, {10, 25, 50},
                {10, 22, 38}, {10, 24, 45}, {10, 25, 48}, {10, 25, 50}, {10, 25, 50}, {10, 25, 45}, {10, 25, 46}, {10, 24, 47},
                {10, 24, 47}, {10, 24, 47}, {10, 24, 47}, {10, 24, 49}, {10, 23, 44}, {10, 23, 45}, {10, 23, 42}, {10, 23, 43},
                {10, 23, 43}, {10, 23, 36}, {10, 24, 44}, {10, 24, 42}, {10, 24, 42}, {10, 24, 45}
        }, 30),
        BAD_BLOOD(new int[][]{
                {10, 22}, {10, 22}, {10, 22}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34},
                {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34},
                {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34},
                {10, 22, 34}, {10, 24, 38}, {10, 24, 38}, {10, 22, 34}, {10, 24, 38}, {10, 22, 34}
        }, 30),
        ALIEN_ARCADIUM(new int[][]{
                {10, 13, 16, 19}, {10, 14, 18, 22}, {10, 13, 16, 19}, {10, 14, 17, 21, 25, 28}, {10, 14, 18, 22, 26, 30},
                {10, 14, 19, 23, 28, 32}, {10, 15, 19, 23, 27, 31}, {10, 15, 20, 25, 30, 35}, {10, 14, 19, 23, 28, 32},
                {10, 16, 22, 27, 33, 38}, {10, 16, 21, 27, 32, 38}, {10, 16, 22, 28, 34, 40}, {10, 16, 22, 28, 34, 40},
                {10, 16, 21, 26, 31, 36}, {10, 17, 24, 31, 38, 46}, {10, 16, 22, 27, 33, 38}, {10, 14, 19, 23, 28, 32},
                {10, 14, 19, 23, 28, 32}, {10, 14, 18, 22, 26, 30}, {10, 15, 21, 26, 31, 36}, {10, 14, 19, 23, 28, 32},
                {10, 14, 19, 23, 28, 34}, {10, 14, 18, 22, 26, 30}, {10, 14, 19, 23, 28, 32}, {10},
                {10, 23, 36}, {10, 22, 34}, {10, 20, 30}, {10, 24, 38}, {10, 22, 34}, {10, 22, 34}, {10, 21, 32}, {10, 22, 34},
                {10, 22, 34}, {10}, {10, 22, 34}, {10, 20, 31}, {10, 22, 34}, {10, 22, 34}, {10, 22, 34, 37, 45}, {10, 21, 32},
                {10, 22, 34}, {10, 13, 22, 25, 34, 37}, {10, 22, 34}, {10, 22, 34, 35}, {10, 21, 32, 35}, {10, 20, 30},
                {10, 20, 30, 33}, {10, 21, 32}, {10, 22, 34, 37}, {10, 20, 30, 33}, {10, 22, 34, 37}, {10, 22, 34, 37},
                {10, 20, 32, 35, 39}, {10, 16, 22, 28, 34, 40}, {10, 14, 18}, {10, 14, 18}, {10, 22, 34, 37, 38},
                {10, 14, 18, 22, 26, 30}, {10, 20, 30, 33}, {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30},
                {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30},
                {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30},
                {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30},
                {10, 14, 18, 22, 27, 32}, {10, 14, 18, 22, 27, 32}, {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30},
                {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30},
                {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30},
                {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30},
                {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30},
                {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30},
                {10, 14, 18, 22, 26, 30}, {10, 14, 18, 22, 26, 30}, {5}, {5}, {5}, {5}, {5}
        }, 105);

        private final int[][] timer;
        private final int maxRound;

        ZombiesMap(int[][] timer, int maxRound) {
            this.timer = timer;
            this.maxRound = maxRound;
        }

        public int[][] getTimer() {
            if (timer == null) {
                return new int[][]{};
            }
            return timer.clone();
        }

        public int getMaxRound() {
            return maxRound;
        }
    }
}


