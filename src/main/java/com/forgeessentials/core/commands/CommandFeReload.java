package com.forgeessentials.core.commands;

import net.minecraft.command.CommandSource;
import net.minecraftforge.server.permission.DefaultPermissionLevel;

import com.forgeessentials.core.ForgeEssentials;
import com.forgeessentials.core.misc.Translator;
import com.forgeessentials.core.moduleLauncher.ModuleLauncher;
import com.forgeessentials.util.output.ChatOutputHandler;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public class CommandFeReload extends ForgeEssentialsCommandBuilder
{

    public CommandFeReload(boolean enabled)
    {
        super(enabled);
    }

    @Override
    public String getPrimaryAlias()
    {
        return "fereload";
    }

    @Override
    public String[] getDefaultSecondaryAliases()
    {
        return new String[] { "reload" };
    }

    @Override
    public String getPermissionNode()
    {
        return ForgeEssentials.PERM_RELOAD;
    }

    @Override
    public DefaultPermissionLevel getPermissionLevel()
    {
        return DefaultPermissionLevel.OP;
    }

    @Override
    public boolean canConsoleUseCommand()
    {
        return true;
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> setExecution()
    {
        return baseBuilder
                .executes(CommandContext -> execute(CommandContext, "blank")
                        );
    }

    @Override
    public int execute(CommandContext<CommandSource> ctx, String params) throws CommandSyntaxException
    {
        reload(ctx.getSource());
        return Command.SINGLE_SUCCESS;
    }

    public static void reload(CommandSource sender)
    {
        ModuleLauncher.instance.reloadConfigs();
        ChatOutputHandler.chatConfirmation(sender, Translator.translate("Reloaded configs. (may not work for all settings)"));
    }
}
