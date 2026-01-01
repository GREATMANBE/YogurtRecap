package com.yogurt.recap.commands;

import com.yogurt.recap.features.killsgoldtracker.KillsGoldTracker;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class RecapCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "recap";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/recap <on|off>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // Anyone can use this command
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            boolean enabled = KillsGoldTracker.isChatMessagesEnabled();
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.YELLOW + "[YogurtRecap] " +
                    EnumChatFormatting.WHITE + "Chat messages are currently " +
                    (enabled ? EnumChatFormatting.GREEN + "enabled" : EnumChatFormatting.RED + "disabled") +
                    EnumChatFormatting.WHITE + "."
            ));
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.GRAY + "Use " + EnumChatFormatting.YELLOW + "/recap on" +
                    EnumChatFormatting.GRAY + " or " + EnumChatFormatting.YELLOW + "/recap off" +
                    EnumChatFormatting.GRAY + " to toggle."
            ));
            return;
        }

        String arg = args[0].toLowerCase();
        if (arg.equals("on") || arg.equals("enable") || arg.equals("true")) {
            KillsGoldTracker.setChatMessagesEnabled(true);
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.YELLOW + "[YogurtRecap] " +
                    EnumChatFormatting.GREEN + "Chat messages enabled."
            ));
        } else if (arg.equals("off") || arg.equals("disable") || arg.equals("false")) {
            KillsGoldTracker.setChatMessagesEnabled(false);
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.YELLOW + "[YogurtRecap] " +
                    EnumChatFormatting.RED + "Chat messages disabled."
            ));
        } else {
            throw new CommandException("Invalid argument. Use 'on' or 'off'.");
        }
    }
}




