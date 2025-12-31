package com.yogurt.recap.utils;

import com.yogurt.recap.YogurtRecapMod;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public final class PlayerUtils {
    private PlayerUtils() {}

    public static boolean isInZombiesTitle() {
        return LanguageUtils.isZombiesTitle(YogurtRecapMod.getScoreboardManager().getTitle());
    }

    public static void sendMessage(String string) {
        sendMessage(new ChatComponentText(string));
    }

    public static void sendMessage(IChatComponent component) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.thePlayer != null) {
            mc.thePlayer.addChatComponentMessage(component);
        }
    }
}


