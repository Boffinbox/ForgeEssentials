package com.forgeessentials.playerlogger;

import java.sql.Blob;
import java.util.Date;

import javax.persistence.EntityManager;

import com.forgeessentials.api.UserIdent;
import com.forgeessentials.playerlogger.entity.BlockData;
import com.forgeessentials.playerlogger.entity.PlayerData;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;

public abstract class PlayerLoggerEvent<T>
{
    public Date date;

    public T event;

    public PlayerLoggerEvent(T event)
    {
        this.event = event;
        this.date = new Date();
    }

    public abstract void process(EntityManager em);

    public BlockData getBlock(Block block)
    {
        return ModulePlayerLogger.getLogger().getBlock(block);
    }

    // public WorldData getWorld(String dimensionId)
    // {
    // return ModulePlayerLogger.getLogger().getWorld(dimensionId);
    // }

    public PlayerData getPlayer(UserIdent ident)
    {
        return ModulePlayerLogger.getLogger().getPlayer(ident.getUuid(), ident.getUsername());
    }

    public PlayerData getPlayer(PlayerEntity player)
    {
        return ModulePlayerLogger.getLogger().getPlayer(player.getGameProfile().getId(), player.getDisplayName().getString());
    }

    public Blob getTileEntityBlob(TileEntity tileEntity)
    {
        return PlayerLogger.tileEntityToBlob(tileEntity);
    }

}