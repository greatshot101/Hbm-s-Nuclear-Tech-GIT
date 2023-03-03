package com.hbm.tileentity.machine;

import java.util.Random;

import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemKeyPin;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.interfaces.ILaserable;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemCrucible;
import com.hbm.packet.AuxParticlePacket;
import com.hbm.packet.PacketDispatcher;
import com.hbm.inventory.DFCRecipes;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class TileEntityCrateDesh extends TileEntityLockableBase implements ITickable, ILaserable {

	public ItemStackHandler inventory;
	private String customName;

	private Random rand = new Random();

	public int heatTimer;

	public TileEntityCrateDesh() {
		inventory = new ItemStackHandler(104){
			@Override
			protected void onContentsChanged(int slot){
				markDirty();
			}
		};
	}

	public boolean canAccess(EntityPlayer player) {
		
		if(!this.isLocked() || player == null) {
			return true;
		} else {
			ItemStack stack = player.getHeldItemMainhand();
			
			if(stack.getItem() instanceof ItemKeyPin && ItemKeyPin.getPins(stack) == this.lock) {
	        	world.playSound(null, player.posX, player.posY, player.posZ, HBMSoundHandler.lockOpen, SoundCategory.BLOCKS, 1.0F, 1.0F);
				return true;
			}
			
			if(stack.getItem() == ModItems.key_red) {
	        	world.playSound(null, player.posX, player.posY, player.posZ, HBMSoundHandler.lockOpen, SoundCategory.BLOCKS, 1.0F, 1.0F);
				return true;
			}
			
			return this.tryPick(player);
		}
	}

	public String getInventoryName() {
		return this.hasCustomInventoryName() ? this.customName : "container.crateDesh";
	}

	public boolean hasCustomInventoryName() {
		return this.customName != null && this.customName.length() > 0;
	}

	public void setCustomName(String name) {
		this.customName = name;
	}

	public boolean isUseableByPlayer(EntityPlayer player) {
		if (world.getTileEntity(pos) != this) {
			return false;
		} else {
			return player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64;
		}
	}

	@Override
	public void update() {
		
		if(!world.isRemote) {
			if(heatTimer > 0)
				heatTimer--;
	
			if(heatTimer > 0) {
				PacketDispatcher.wrapper.sendToAllAround(new AuxParticlePacket(pos.getX(), pos.getY(), pos.getZ(), 4), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 50));
			}
		}
	}

	@Override
	public void addEnergy(long energy, EnumFacing dir) {
		heatTimer = 5;
		
		for(int i = 0; i < inventory.getSlots(); i++) {
			
			if(inventory.getStackInSlot(i).isEmpty())
				continue;
			
			ItemStack result = FurnaceRecipes.instance().getSmeltingResult(inventory.getStackInSlot(i));

			long requiredEnergy = DFCRecipes.getRequiredFlux(inventory.getStackInSlot(i));
			if(requiredEnergy > -1 && energy > requiredEnergy){
				if(0.0001D > rand.nextDouble()*((double)requiredEnergy/(double)energy)){
					result = DFCRecipes.getOutput(inventory.getStackInSlot(i));
				}
			}
			
			if(inventory.getStackInSlot(i).getItem() == ModItems.crucible && ItemCrucible.getCharges(inventory.getStackInSlot(i)) < 3 && energy > 10000000)
				ItemCrucible.charge(inventory.getStackInSlot(i));
			
			if(result != null && !result.isEmpty()){
				int size = inventory.getStackInSlot(i).getCount();
			
				if(result.getCount() * size <= result.getMaxStackSize()) {
					inventory.setStackInSlot(i, result.copy());
					inventory.getStackInSlot(i).setCount(inventory.getStackInSlot(i).getCount()*size);
				}
			}
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound compound) {
		if(compound.hasKey("inventory"))
			inventory.deserializeNBT(compound.getCompoundTag("inventory"));
		super.readFromNBT(compound);
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setTag("inventory", inventory.serializeNBT());
		return super.writeToNBT(compound);
	}
	
	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
	}
	
	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory) : super.getCapability(capability, facing);
	}
}
