package com.forgeessentials.commands.item;

import net.minecraft.command.CommandException;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.permission.DefaultPermissionLevel;

import com.forgeessentials.commands.ModuleCommands;
import com.forgeessentials.core.commands.BaseCommand;
import com.forgeessentials.core.misc.TranslatedCommandException;

public class CommandRename extends BaseCommand
{

    @Override
    public String getPrimaryAlias()
    {
        return "rename";
    }

    @Override
    public boolean canConsoleUseCommand()
    {
        return false;
    }

    @Override
    public DefaultPermissionLevel getPermissionLevel()
    {
        return DefaultPermissionLevel.OP;
    }

    @Override
    public String getPermissionNode()
    {
        return ModuleCommands.PERM + ".rename";
    }

    @Override
    public void processCommandPlayer(MinecraftServer server, ServerPlayerEntity sender, String[] args) throws CommandException
    {
        if (args.length == 0)
            throw new TranslatedCommandException(getUsage(sender));

        ItemStack is = sender.inventory.getSelected();
        if (is == ItemStack.EMPTY)
            throw new TranslatedCommandException("You are not holding a valid item.");

        StringBuilder sb = new StringBuilder();
        for (String arg : args)
        {
            sb.append(arg + " ");
        }
        is.setStackDisplayName(sb.toString().trim());
    }

}