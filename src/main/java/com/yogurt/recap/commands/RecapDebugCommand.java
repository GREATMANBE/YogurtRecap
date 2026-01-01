package com.yogurt.recap.commands;

import com.yogurt.recap.features.killsgoldtracker.KillsGoldTracker;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class RecapDebugCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "recapdebug";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/recapdebug <on|off>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // Anyone can use this command
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            boolean enabled = KillsGoldTracker.isDebugEnabled();
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.YELLOW + "[YogurtRecap] " +
                    EnumChatFormatting.WHITE + "Debug mode is currently " +
                    (enabled ? EnumChatFormatting.GREEN + "enabled" : EnumChatFormatting.RED + "disabled") +
                    EnumChatFormatting.WHITE + "."
            ));
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.GRAY + "Use " + EnumChatFormatting.YELLOW + "/recapdebug on" +
                    EnumChatFormatting.GRAY + " or " + EnumChatFormatting.YELLOW + "/recapdebug off" +
                    EnumChatFormatting.GRAY + " to toggle."
            ));
            return;
        }

        String arg = args[0].toLowerCase();
        if (arg.equals("on") || arg.equals("enable") || arg.equals("true")) {
            KillsGoldTracker.setDebugEnabled(true);
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.YELLOW + "[YogurtRecap] " +
                    EnumChatFormatting.GREEN + "Debug mode enabled."
            ));
        } else if (arg.equals("off") || arg.equals("disable") || arg.equals("false")) {
            KillsGoldTracker.setDebugEnabled(false);
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.YELLOW + "[YogurtRecap] " +
                    EnumChatFormatting.RED + "Debug mode disabled."
            ));
        } else {
            throw new CommandException("Invalid argument. Use 'on' or 'off'.");
        }
    }
}

