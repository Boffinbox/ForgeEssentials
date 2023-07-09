package com.forgeessentials.jscripting.wrapper.mc.item;

import com.forgeessentials.jscripting.wrapper.JsWrapper;

import net.minecraft.inventory.IInventory;

public class JsInventory<T extends IInventory> extends JsWrapper<T>
{

    /**
     * @tsd.ignore
     */
    public static <T extends IInventory> JsInventory<T> get(T inventory)
    {
        return inventory == null ? null : new JsInventory<T>(inventory);
    }

    protected JsInventory(T that)
    {
        super(that);
    }

    public JsItemStack getStackInSlot(int slot)
    {
        return JsItemStack.get(that.getItem(slot));
    }

    public void setStackInSlot(int slot, JsItemStack stack)
    {
        that.setItem(slot, stack.getThat());
    }

    public boolean isStackValidForSlot(int slot, JsItemStack stack)
    {
        return that.canPlaceItem(slot, stack.getThat());
    }

    public int getSize()
    {
        return that.getContainerSize();
    }

    public int getStackLimit()
    {
        return that.getMaxStackSize();
    }
}
