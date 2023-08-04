package com.forgeessentials.serverNetwork.packetbase.packets;

import com.forgeessentials.serverNetwork.packetbase.FEPacket;
import com.forgeessentials.serverNetwork.packetbase.PacketHandler;

import net.minecraft.network.PacketBuffer;

public class Packet10SharedCommandSending extends FEPacket
{
    String commandToSend;

    public Packet10SharedCommandSending() {}
    
    public Packet10SharedCommandSending(String command){
        this.commandToSend = command;
        
    }
    @Override
    public void encode(PacketBuffer buf)
    {
        buf.writeUtf(commandToSend);
    }

    @Override
    public void decode(PacketBuffer buf)
    {
        commandToSend = buf.readUtf();
    }

    @Override
    public void handle(PacketHandler packetHandler)
    {
        packetHandler.handle(this);
    }

    @Override
    public int getID()
    {
        return 10;
    }

    public String getCommandToSend()
    {
        return commandToSend;
    }
    
}
