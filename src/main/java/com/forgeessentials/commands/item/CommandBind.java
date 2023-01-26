package com.forgeessentials.commands.item;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.MessageArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickEmpty;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickEmpty;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.server.permission.DefaultPermissionLevel;

import com.forgeessentials.commands.ModuleCommands;
import com.forgeessentials.core.commands.BaseCommand;
import com.forgeessentials.core.misc.TranslatedCommandException;
import com.forgeessentials.core.misc.Translator;
import com.forgeessentials.util.ItemUtil;
import com.forgeessentials.util.output.ChatOutputHandler;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public class CommandBind extends BaseCommand
{

    public CommandBind(String name, int permissionLevel, boolean enabled)
    {
        super(name, permissionLevel, enabled);
    }

    private static final String TAG_NAME = "FEbinding";

    public static final String LORE_TEXT_TAG = TextFormatting.RESET.toString() + TextFormatting.AQUA;

    @Override
    public String getPrimaryAlias()
    {
        return "bind";
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
        return ModuleCommands.PERM + ".bind";
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> setExecution()
    {
        return builder
                .then(Commands.literal("left")
                        .then(Commands.literal("command")
                                .then(Commands.argument("command", MessageArgument.message())
                                        .executes(CommandContext -> execute(CommandContext, "left")
                                                )
                                        )
                                )
                        .then(Commands.literal("none")
                                .then(Commands.argument("command", MessageArgument.message())
                                        .executes(CommandContext -> execute(CommandContext, "left")
                                                )
                                        )
                                )
                        )
                .then(Commands.literal("right")
                        .then(Commands.literal("command")
                                .then(Commands.argument("command", MessageArgument.message())
                                        .executes(CommandContext -> execute(CommandContext, "right")
                                                )
                                        )
                                )
                        .then(Commands.literal("none")
                                .then(Commands.argument("command", MessageArgument.message())
                                        .executes(CommandContext -> execute(CommandContext, "right")
                                                )
                                        )
                                )
                        )
                .then(Commands.literal("clear")
                        .executes(CommandContext -> execute(CommandContext, "clear")
                                )
                        )
                .executes(CommandContext -> execute(CommandContext, "blank")
                        );
    }

    @Override
    public int execute(CommandContext<CommandSource> ctx, Object... params) throws CommandSyntaxException
    {
        if (params.toString() =="blank")
        {
            ChatOutputHandler.chatConfirmation(ctx.getSource(),"/bind <left|right> <command...>: Bind command to an item");
            return Command.SINGLE_SUCCESS;
        }

        String side = params.toString();

        // If sub-command is "clear"
        if (side.equals("clear"))
        {
            ItemStack is = ((PlayerEntity) ctx.getSource().getEntity()).getMainHandItem();
            if (is == ItemStack.EMPTY)
                throw new TranslatedCommandException("You are not holding a valid item.");
            CompoundNBT tag = is.getTag();
            if (tag != null)
                tag.remove(TAG_NAME);
            ChatOutputHandler.chatConfirmation(ctx.getSource(), "Cleared bound commands from item");
            return Command.SINGLE_SUCCESS;
        }

        String option = MessageArgument.getMessage(ctx, "command").getString();

        if (option.isEmpty())
        {
            ChatOutputHandler.chatConfirmation(ctx.getSource(), Translator.format("/bind " + side + " <command...>: Bind command to an item"));
            ChatOutputHandler.chatConfirmation(ctx.getSource(), Translator.format("/bind " + side + " none: Clear bound command"));
            return Command.SINGLE_SUCCESS;
        }

        ItemStack is = ((PlayerEntity) ctx.getSource().getEntity()).getMainHandItem();
        if (is == ItemStack.EMPTY)
            throw new TranslatedCommandException("You are not holding a valid item.");
        CompoundNBT tag = ItemUtil.getTagCompound(is);
        CompoundNBT bindTag = ItemUtil.getCompoundTag(tag, TAG_NAME);
        CompoundNBT display = tag.getCompound("display");

        if (option.equals("none"))
        {
            bindTag.remove(side);
            display.put("Lore", new ListNBT());
            ChatOutputHandler.chatConfirmation(ctx.getSource(), Translator.format("Cleared " + side + " bound command from item"));
        }
        else
        {
            String command = option;
            bindTag.putString(side, command);

            String loreStart = LORE_TEXT_TAG + side + "> ";
            StringNBT loreTag = StringNBT.valueOf(loreStart + command);

            ListNBT lore = display.getList("Lore", 9);
            for (int i = 0; i < lore.size(); ++i)
            {
                if (lore.getString(i).startsWith(loreStart))
                {
                    lore.set(i, loreTag);
                    ChatOutputHandler.chatConfirmation(ctx.getSource(), "Bound command to item");
                    return Command.SINGLE_SUCCESS;
                }
            }
            lore.add(loreTag);
            display.put("Lore", lore);
            tag.put("display", display);
        }
        ChatOutputHandler.chatConfirmation(ctx.getSource(), "Bound command to item");
        return Command.SINGLE_SUCCESS;
    }

    @SubscribeEvent
    public void playerInteractEvent(PlayerInteractEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayerEntity))
            return;
        ItemStack stack = event.getPlayer().getMainHandItem();
        if (stack == ItemStack.EMPTY || stack.getTag() == null || !stack.getTag().contains(TAG_NAME))
            return;
        CompoundNBT nbt = stack.getTag().getCompound(TAG_NAME);

        String command;
        if (event instanceof LeftClickBlock || event instanceof LeftClickEmpty)
            command = nbt.getString("left");
        else if (event instanceof RightClickBlock || event instanceof RightClickEmpty)
            command = nbt.getString("right");
        else
            return;
        if (command == null || command.isEmpty())
            return;

        ServerLifecycleHooks.getCurrentServer().getCommands().performCommand(event.getPlayer().createCommandSourceStack(), command);
        event.setCanceled(true);
    }
}