package com.forgeessentials.permissions.commands;

import static com.forgeessentials.permissions.core.ItemPermissionManager.TAG_MODE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.forgeessentials.api.APIRegistry;
import com.forgeessentials.api.permissions.FEPermissions;
import com.forgeessentials.api.permissions.Zone;
import com.forgeessentials.core.commands.ForgeEssentialsCommandBuilder;
import com.forgeessentials.permissions.core.ItemPermissionManager;
import com.forgeessentials.util.ItemUtil;
import com.forgeessentials.util.output.ChatOutputHandler;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import org.jetbrains.annotations.NotNull;

public class CommandPermissionTaggedItem extends ForgeEssentialsCommandBuilder
{

    public CommandPermissionTaggedItem(boolean enabled)
    {
        super(enabled);
    }

    @Override
    public @NotNull String getPrimaryAlias()
    {
        return "permtaggeditem";
    }

    @Override
    public DefaultPermissionLevel getPermissionLevel()
    {
        return DefaultPermissionLevel.OP;
    }

    @Override
    public boolean canConsoleUseCommand()
    {
        return false;
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> setExecution()
    {
        return baseBuilder
                .then(Commands.literal("mode")
                        .then(Commands.literal("inventory").executes(context -> execute(context, "mode-inventory")))
                        .then(Commands.literal("equip").executes(context -> execute(context, "mode-equip")))
                        .then(Commands.literal("use").executes(context -> execute(context, "mode-use"))))
                .then(Commands.literal("perm")
                        .then(Commands.argument("perm", StringArgumentType.string()).suggests(SUGGEST_PERMS)
                                .then(Commands.literal(Zone.PERMISSION_TRUE)
                                        .executes(context -> execute(context, "perm-" + Zone.PERMISSION_TRUE)))
                                .then(Commands.literal(Zone.PERMISSION_FALSE)
                                        .executes(context -> execute(context, "perm-" + Zone.PERMISSION_FALSE)))))
                .then(Commands.literal("group")
                        .then(Commands.argument("group", StringArgumentType.string()).suggests(SUGGEST_GROUPS)
                                .executes(context -> execute(context, "group"))))
                .then(Commands.literal("reset").executes(context -> execute(context, "reset")));
    }

    public static final SuggestionProvider<CommandSource> SUGGEST_GROUPS = (ctx, builder) -> {
        List<String> completeList = new ArrayList<>(APIRegistry.perms.getServerZone().getGroups());
        return ISuggestionProvider.suggest(completeList, builder);
    };

    public static final SuggestionProvider<CommandSource> SUGGEST_PERMS = (ctx, builder) -> {
        List<String> listperm = new ArrayList<>(APIRegistry.perms.getServerZone().getRootZone().enumRegisteredPermissions());
        for (int index = 0; index < listperm.size(); index++)
        {
            if (listperm.get(index).contains("*"))
            {
                listperm.set(index, listperm.get(index).replace("*", "+"));
            }
        }
        return ISuggestionProvider.suggest(listperm, builder);
    };

    @Override
    public int processCommandPlayer(CommandContext<CommandSource> ctx, String params) throws CommandSyntaxException
    {
        if(!ItemPermissionManager.isEnabled()) {
        	ChatOutputHandler.chatError(ctx.getSource(), "Command requires ItemPermissionManager to be enabled!");
            return Command.SINGLE_SUCCESS;
        }
        ItemStack stack = getServerPlayer(ctx.getSource()).getMainHandItem();
        if (stack == ItemStack.EMPTY)
        {
            ChatOutputHandler.chatError(ctx.getSource(), "No item equipped!");
            return Command.SINGLE_SUCCESS;
        }

        if (params.equals("help"))
        {
            ChatOutputHandler.chatConfirmation(ctx.getSource(), "/permitem mode inventory|equip"); // |use");
            ChatOutputHandler.chatConfirmation(ctx.getSource(), "/permitem perm <perm> <value>");
            ChatOutputHandler.chatConfirmation(ctx.getSource(), "/permitem group <group>");
            ChatOutputHandler.chatConfirmation(ctx.getSource(), "/permitem reset");
            return Command.SINGLE_SUCCESS;
        }

        String[] subCmd = params.split("-");
        switch (subCmd[0])
        {
        case "mode":
            parseMode(ctx, stack, subCmd);
            break;
        case "perm":
            parsePermission(ctx, stack, subCmd);
            break;
        case "group":
            parseGroup(ctx, stack, subCmd);
            break;
        case "reset":
            CompoundNBT stackTag = stack.getTag();
            if (stackTag != null)
                stackTag.remove(ItemPermissionManager.TAG_BASE);
            ChatOutputHandler.chatConfirmation(ctx.getSource(), "Deleted permission item settings");
            break;
        default:
            ChatOutputHandler.chatError(ctx.getSource(), FEPermissions.MSG_UNKNOWN_SUBCOMMAND, Arrays.toString(subCmd));
        }
        return Command.SINGLE_SUCCESS;
    }

    public static void parseMode(CommandContext<CommandSource> ctx, ItemStack stack, String[] params)
            throws CommandException
    {
        String subCmd = params[1];

        switch (subCmd)
        {
        case "inventory":
            getOrCreatePermissionTag(stack).putByte(TAG_MODE, ItemPermissionManager.MODE_INVENTORY);
            break;
        case "equip":
            getOrCreatePermissionTag(stack).putByte(TAG_MODE, ItemPermissionManager.MODE_EQUIP);
            break;
        case "use":
            getOrCreatePermissionTag(stack).putByte(TAG_MODE, ItemPermissionManager.MODE_USE);
            break;
        default:
            ChatOutputHandler.chatError(ctx.getSource(), FEPermissions.MSG_UNKNOWN_SUBCOMMAND, subCmd);
        }
    }

    public static void parsePermission(CommandContext<CommandSource> ctx, ItemStack stack, String[] params)
            throws CommandException
    {
        String permission = StringArgumentType.getString(ctx, "perm");

        String value = params[1];

        getSettingsTag(stack).add(StringNBT.valueOf(permission + "=" + value));
        ChatOutputHandler.chatConfirmation(ctx.getSource(), "Set permission %s=%s for item", permission, value);
    }

    public static void parseGroup(CommandContext<CommandSource> ctx, ItemStack stack, String[] params)
            throws CommandException
    {
        String group = StringArgumentType.getString(ctx, "group");

        getSettingsTag(stack).add(StringNBT.valueOf(group));
        ChatOutputHandler.chatConfirmation(ctx.getSource(), "Added group %s to item", group);
    }

    public static CompoundNBT getOrCreatePermissionTag(ItemStack stack)
    {
        CompoundNBT stackTag = ItemUtil.getTagCompound(stack);
        CompoundNBT tag = stackTag.getCompound(ItemPermissionManager.TAG_BASE);
        stackTag.put(ItemPermissionManager.TAG_BASE, tag);
        return tag;
    }

    public static ListNBT getSettingsTag(ItemStack stack)
    {
        CompoundNBT tag = getOrCreatePermissionTag(stack);
        ListNBT settings = ItemPermissionManager.getSettingsTag(tag);
        tag.put(ItemPermissionManager.TAG_SETTINGS, settings);
        return settings;
    }

}
