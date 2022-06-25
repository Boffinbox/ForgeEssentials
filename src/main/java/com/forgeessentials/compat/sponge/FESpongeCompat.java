package com.forgeessentials.compat.sponge;

import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.*;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import com.forgeessentials.commons.BuildInfo;
import com.forgeessentials.compat.sponge.economy.FEEconService;
import com.forgeessentials.core.environment.Environment;
import com.google.inject.Inject;

/**
 * Plugin class for FE-Sponge compatibility.
 *
 * Watch this space, more to come.
 */
@Plugin(value = "forgeessentials-sponge")
public class FESpongeCompat
{

    @Inject
    private Game game;

    @Listener
    public void checkEnvironment(GameConstructionEvent e)
    {
        if (!game.platform().getImplementation().getName().equals("SpongeForge"))
        {
            throw new RuntimeException("You must be running the Forge implementation of SpongeAPI on Minecraft Forge in order to load ForgeEssentials!");
        }
    }

    @Listener
    public void register(GamePreInitializationEvent e)
    {
        Environment.registerSpongeCompatPlugin(game.pluginManager().isLoaded("worldedit"));
    }

    @Listener
    public void init(GameInitializationEvent e)
    {
        Sponge.getServiceManager().setProvider(this, EconomyService.class, new FEEconService());
    }

}
