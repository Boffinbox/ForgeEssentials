package com.forgeessentials.commands.util;

import com.forgeessentials.commands.player.CommandNoClip;
import com.forgeessentials.util.events.ServerEventHandler;

import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CommandsEventHandler extends ServerEventHandler {

	public static int getWorldHour(World world) {
		return (int) ((world.getDayTime() % 24000) / 1000);
	}

	public static int getWorldDays(World world) {
		return (int) (world.getDayTime() / 24000);
	}

	public static void makeWorldTimeHours(World world, int target) {
		world.getTimeOfDay((getWorldDays(world) + 1) * 24000 + (target * 1000));
	}

	public CommandsEventHandler() {
		super();
	}

	@SubscribeEvent
	public void tickStart(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END)
			CommandNoClip.checkClip(event.player);
	}

}
