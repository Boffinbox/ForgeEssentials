package com.forgeessentials.core.misc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.command.CommandException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fe.event.entity.EntityPortalEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import com.forgeessentials.api.APIRegistry;
import com.forgeessentials.api.UserIdent;
import com.forgeessentials.api.permissions.Zone;
import com.forgeessentials.commons.selections.WarpPoint;
import com.forgeessentials.commons.selections.WorldPoint;
import com.forgeessentials.core.ForgeEssentials;
import com.forgeessentials.util.PlayerInfo;
import com.forgeessentials.util.ServerUtil;
import com.forgeessentials.util.events.PlayerChangedZone;
import com.forgeessentials.util.events.ServerEventHandler;
import com.forgeessentials.util.output.ChatOutputHandler;
import com.forgeessentials.util.output.LoggingHandler;

public class TeleportHelper extends ServerEventHandler
{

    public static class SimpleTeleporter extends Teleporter
    {

        public SimpleTeleporter(ServerWorld world)
        {
            super(world);
        }

        @Override
        public void placeInPortal(Entity entity, float yaw)
        {
            int i = MathHelper.floor(entity.posX);
            int j = MathHelper.floor(entity.posY) - 1;
            int k = MathHelper.floor(entity.posZ);
            entity.setLocationAndAngles(i, j, k, entity.rotationYaw, 0.0F);
        }

        @Override
        public void removeStalePortalLocations(long totalWorldTime)
        {
            /* do nothing */
        }

        @Override
        public boolean placeInExistingPortal(Entity entity, float yaw)
        {
            placeInPortal(entity, yaw);
            return true;
        }

    }

    public static class TeleportInfo
    {

        private PlayerEntity player;

        private long start;

        private int timeout;

        private WarpPoint point;

        private WarpPoint playerPos;

        public TeleportInfo(PlayerEntity player, WarpPoint point, int timeout)
        {
            this.point = point;
            this.timeout = timeout;
            this.start = System.currentTimeMillis();
            this.player = player;
            this.playerPos = new WarpPoint(player);
        }

        public boolean check()
        {
            if (playerPos.distance(new WarpPoint(player)) > 0.2)
            {
                ChatOutputHandler.chatWarning(player, "Teleport cancelled.");
                return true;
            }
            if (System.currentTimeMillis() - start < timeout)
            {
                return false;
            }
            checkedTeleport(player, point);
            ChatOutputHandler.chatConfirmation(player, "Teleported.");
            return true;
        }

    }

    public static final String TELEPORT_COOLDOWN = "fe.teleport.cooldown";
    public static final String TELEPORT_WARMUP = "fe.teleport.warmup";
    public static final String TELEPORT_CROSSDIM_FROM = "fe.teleport.crossdim.from";
    public static final String TELEPORT_CROSSDIM_TO = "fe.teleport.crossdim.to";
    public static final String TELEPORT_CROSSDIM_PORTALFROM = "fe.teleport.crossdim.portalfrom";
    public static final String TELEPORT_CROSSDIM_PORTALTO = "fe.teleport.crossdim.portalto";
    public static final String TELEPORT_FROM = "fe.teleport.from";
    public static final String TELEPORT_TO = "fe.teleport.to";
    public static final String TELEPORT_PORTALFROM = "fe.teleport.portalfrom";
    public static final String TELEPORT_PORTALTO = "fe.teleport.portalto";

    private static Map<UUID, TeleportInfo> tpInfos = new HashMap<>();

    public static void teleport(PlayerEntity player, WarpPoint point) throws CommandException
    {
        if (point.getWorld() == null)
        {
            ServerLifecycleHooks.getCurrentServer().getLevel(point.getDimension());
            if (point.getWorld() == null)
            {
                ChatOutputHandler.chatError(player, Translator.translate("Unable to teleport! Target dimension does not exist"));
                return;
            }
        }

        // Check permissions
        UserIdent ident = UserIdent.get(player);
        if (!APIRegistry.perms.checkPermission(player, TELEPORT_FROM))
            throw new TranslatedCommandException("You are not allowed to teleport from here.");
        if (!APIRegistry.perms.checkUserPermission(ident, point.toWorldPoint(), TELEPORT_TO))
            throw new TranslatedCommandException("You are not allowed to teleport to that location.");
        if (player.level.dimension() != point.getDimension())
        {
            if (!APIRegistry.perms.checkPermission(player, TELEPORT_CROSSDIM_FROM))
                throw new TranslatedCommandException("You are not allowed to teleport from this dimension.");
            if (!APIRegistry.perms.checkUserPermission(ident, point.toWorldPoint(), TELEPORT_CROSSDIM_TO))
                throw new TranslatedCommandException("You are not allowed to teleport to that dimension.");
        }

        // Get and check teleport cooldown
        int teleportCooldown = ServerUtil.parseIntDefault(APIRegistry.perms.getUserPermissionProperty(ident, TELEPORT_COOLDOWN), 0) * 1000;
        if (teleportCooldown > 0)
        {
            PlayerInfo pi = PlayerInfo.get(player);
            long cooldownDuration = (pi.getLastTeleportTime() + teleportCooldown) - System.currentTimeMillis();
            if (cooldownDuration >= 0)
            {
                ChatOutputHandler.chatNotification(player, Translator.format("Cooldown still active. %d seconds to go.", cooldownDuration / 1000));
                return;
            }
        }

        // Get and check teleport warmup
        int teleportWarmup = ServerUtil.parseIntDefault(APIRegistry.perms.getUserPermissionProperty(ident, TELEPORT_WARMUP), 0);
        if (teleportWarmup <= 0)
        {
            checkedTeleport(player, point);
            return;
        }

        if (!canTeleportTo(point))
        {
            ChatOutputHandler.chatError(player, Translator.translate("Unable to teleport! Target location obstructed.")
                    + String.format(" (%2.2f,%2.2f,%2.2f)", point.getX(), point.getY(), point.getZ()));
            return;
        }

        // Setup timed teleport
        tpInfos.put(player.getUUID(), new TeleportInfo(player, point, teleportWarmup * 1000));
        ChatOutputHandler.chatNotification(player,
                Translator.format("Teleporting. Please stand still for %s.", ChatOutputHandler.formatTimeDurationReadable(teleportWarmup, true)));
    }

    public static boolean canTeleportTo(WarpPoint point)
    {
        // TODO (upgrade): Check this!
        if (!ForgeEssentials.isCubicChunksInstalled && point.getY() < 0)
            return false;
        BlockPos blockPos1 = point.getBlockPos();
        BlockPos blockPos2 = new BlockPos(point.getBlockX(), point.getBlockY() + 1, point.getBlockZ());
        Block block1 = point.getWorld().getBlockState(blockPos1).getBlock();
        Block block2 = point.getWorld().getBlockState(blockPos2).getBlock();
        AxisAlignedBB blockBounds1 = block1.getCollisionBoundingBox(block1.defaultBlockState(), point.getWorld(), blockPos1);
        AxisAlignedBB blockBounds2 = block2.getCollisionBoundingBox(block2.defaultBlockState(), point.getWorld(), blockPos2);
        boolean block1Free = !block1.getMaterial(block1.defaultBlockState()).isSolid() || blockBounds1 == null || blockBounds1.maxX < 1 || blockBounds1.maxY > 0;
        boolean block2Free = !block2.getMaterial(block2.defaultBlockState()).isSolid() || blockBounds2 == null || blockBounds2.maxX < 1 || blockBounds2.maxY > 0;
        return block1Free && block2Free;
    }

    public static void checkedTeleport(PlayerEntity player, WarpPoint point)
    {
        if (!canTeleportTo(point))
        {
            ChatOutputHandler.chatError(player, Translator.translate("Unable to teleport! Target location obstructed.")
                    + String.format(" (%2.2f,%2.2f,%2.2f)", point.getX(), point.getY(), point.getZ()));
            return;
        }

        PlayerInfo pi = PlayerInfo.get(player);
        WarpPoint old = new WarpPoint(player);
        pi.setLastTeleportOrigin(old);
        pi.setLastTeleportTime(System.currentTimeMillis());
        pi.setLastDeathLocation(null);

        doTeleport(player, point);
        Zone before = APIRegistry.perms.getServerZone().getZonesAt(old.toWorldPoint()).get(0);
        Zone after = APIRegistry.perms.getServerZone().getZonesAt(point.toWorldPoint()).get(0);
        MinecraftForge.EVENT_BUS.post(new PlayerChangedZone(player, before, after, old, point));
    }

    public static void doTeleport(PlayerEntity player, WarpPoint point)
    {
        if (point.getWorld() == null)
        {
            LoggingHandler.felog.error("Error teleporting player. Target world is NULL");
            return;
        }
        // TODO: Handle teleportation of mounted entity
        player.stopRiding();
        ;

        if (player.level.dimension() != point.getDimension())
        {
            SimpleTeleporter teleporter = new SimpleTeleporter(point.getWorld());
            MinecraftServer mcServer = ServerLifecycleHooks.getCurrentServer();
            mcServer.getPlayerList().transferPlayerToDimension(player, point.getDimension(), teleporter);
        }
        player.connection.setPlayerLocation(point.getX(), point.getY(), point.getZ(), point.getYaw(), point.getPitch());
    }

    public static void doTeleportEntity(Entity entity, WarpPoint point)
    {
        if (entity instanceof PlayerEntity)
        {
            doTeleport((PlayerEntity) entity, point);
            return;
        }
        if (entity.level.dimension() != point.getDimension())
            entity.changeDimension(point.getWorld());
        entity.setLocationAndAngles(point.getX(), point.getY(), point.getZ(), point.getYaw(), point.getPitch());
    }

    @SubscribeEvent
    public void serverTickEvent(TickEvent.ServerTickEvent e)
    {
        if (e.phase == TickEvent.Phase.START)
        {
            for (Iterator<TeleportInfo> it = tpInfos.values().iterator(); it.hasNext();)
            {
                TeleportInfo tpInfo = it.next();
                if (tpInfo.check())
                {
                    it.remove();
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void entityPortalEvent(EntityPortalEvent e)
    {
        UserIdent ident = null;
        if (e.getEntity() instanceof PlayerEntity)
            ident = UserIdent.get((PlayerEntity) e.getEntity());
        else if (e.getEntity() instanceof LivingEntity)
            ident = APIRegistry.IDENT_NPC;
        WorldPoint pointFrom = new WorldPoint(e.world, e.pos);
        WorldPoint pointTo = new WorldPoint(e.targetDimension, e.target);
        if (!APIRegistry.perms.checkUserPermission(ident, pointFrom, TELEPORT_PORTALFROM))
            e.setCanceled(true);
        if (!APIRegistry.perms.checkUserPermission(ident, pointTo, TELEPORT_PORTALTO))
            e.setCanceled(true);
        if (e.world.dimension() != e.targetDimension.dimension())
        {
            if (!APIRegistry.perms.checkUserPermission(ident, pointFrom, TELEPORT_CROSSDIM_PORTALFROM))
                e.setCanceled(true);
            if (!APIRegistry.perms.checkUserPermission(ident, pointTo, TELEPORT_CROSSDIM_PORTALTO))
                e.setCanceled(true);
        }
    }

    // TODO: Remove method
    public static void transferPlayerToDimension(PlayerEntity player, int dimension, Teleporter teleporter)
    {
        // TODO (upgrade): Check teleportation!
        World oldDim = player.level;
        MinecraftServer mcServer = ServerLifecycleHooks.getCurrentServer();

        WorldSettings oldWorld = mcServer.getLevel(player.level);
        player.dimension = dimension;
        ServerWorld newWorld = mcServer.getLevel(player.level.dimension());
        player.connection.sendPacket(new SPacketRespawn(player.dimension, newWorld.getDifficulty(),
                newWorld.getWorldInfo().getTerrainType(), player.interactionManager.getGameType())); // Forge: Use new dimensions information
        oldWorld.removeEntityDangerously(player);
        player.isDead = false;

        transferEntityToWorld(player, oldDim, oldWorld, newWorld, teleporter);

        mcServer.getPlayerList().preparePlayer(player, oldWorld);
        player.connection.setPlayerLocation(player.posX, player.posY, player.posZ, player.rotationYaw,
                player.rotationPitch);
        player.interactionManager.setWorld(newWorld);
        mcServer.getPlayerList().updateTimeAndWeatherForPlayer(player, newWorld);
        mcServer.getPlayerList().syncPlayerInventory(player);
        Iterator<?> iterator = player.getActivePotionEffects().iterator();
        while (iterator.hasNext())
        {
            PotionEffect potioneffect = (PotionEffect) iterator.next();
            player.connection.sendPacket(new SPacketEntityEffect(player.getEntityId(), potioneffect));
        }
        player.sendPlayerAbilities();
        player.connection.sendPacket(new SPacketSetExperience(player.experience, player.experienceTotal, player.experienceLevel));
        FMLCommonHandler.instance().firePlayerChangedDimensionEvent(player, oldDim, dimension);
    }

    public static void transferEntityToWorld(Entity entity, int oldDim, WorldServer oldWorld, WorldServer newWorld, Teleporter teleporter)
    {
        WorldProvider pOld = oldWorld.provider;
        WorldProvider pNew = newWorld.provider;
        double moveFactor = pOld.getMovementFactor() / pNew.getMovementFactor();
        double d0 = entity.posX * moveFactor;
        double d1 = entity.posZ * moveFactor;
        double d3 = entity.posX;
        double d4 = entity.posY;
        double d5 = entity.posZ;
        float f = entity.rotationYaw;
        d0 = MathHelper.clamp((int) d0, -29999872, 29999872);
        d1 = MathHelper.clamp((int) d1, -29999872, 29999872);
        if (entity.isEntityAlive())
        {
            entity.setLocationAndAngles(d0, entity.posY, d1, entity.rotationYaw, entity.rotationPitch);
            teleporter.placeInPortal(entity, f);
            newWorld.spawnEntity(entity);
            newWorld.updateEntityWithOptionalForce(entity, false);
        }
        entity.setWorld(newWorld);
    }

}
