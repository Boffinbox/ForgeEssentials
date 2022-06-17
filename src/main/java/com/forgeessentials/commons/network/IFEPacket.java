package com.forgeessentials.commons.network;

import java.util.function.Supplier;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkEvent.Context;

public interface IFEPacket
{

    void handle(NetworkEvent.Context context);

    void encode(PacketBuffer buffer);

    static <PACKET extends IFEPacket> void handle(final PACKET message, Supplier<Context> ctx)
    {
        Context context = ctx.get();
        context.enqueueWork(() -> message.handle(context));
        context.setPacketHandled(true);
    }
}