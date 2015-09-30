package net.knuples.eventlog.ingestion;

import java.util.concurrent.LinkedBlockingQueue;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.*;
import org.bukkit.block.*;

import net.knuples.eventlog.EventLogPlugin;
import net.knuples.eventlog.Packet;

public class InvIngest extends AbstractIngestionModule {	

	public InvIngest(EventLogPlugin plugin) {
		super(plugin);
		// TODO Auto-generated constructor stub
	}
	
	private Packet SerializeInventory(Inventory i)
	{
		Packet p = this.GetPacket();
		p.AddData("type", i.getType());
		p.AddData("inv-owner", this.SerializeInventoryHolder(i.getHolder()));
		return p;
	}
	
	private Packet NewInventoryEvent(InventoryEvent e)
	{
		return this.NewEventPacket(e).AddData("inventory", this.SerializeInventory(e.getInventory()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void BrewEvent(BrewEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).
							AddData("block",     this.SerializeBlock(p.getBlock())).
							AddData("contents",  this.SerializeItem(p.getContents().getIngredient())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void FurnaceBurnEvent(FurnaceBurnEvent p)
	{
		if (p.getBurnTime() == 0) return;
		this.AddEventToQueue(this.NewEventPacket(p).
							AddData("block",     this.SerializeBlock(p.getBlock())).
							AddData("time",      p.getBurnTime()).
							AddData("isburning", p.isBurning()).
							AddData("fuel",      this.SerializeItem(p.getFuel())));
	}
	
	public static boolean isSpaceAvailable(Inventory inventory, ItemStack stack)
	{
		try 
		{
			int maxstack = stack.getMaxStackSize();
			for (int slot = 0; slot < inventory.getSize(); slot++) {
				ItemStack citem = inventory.getItem(slot);
				if (citem == null) return true;
				else if ((citem.isSimilar(stack)) && 
						 ((maxstack - citem.getAmount()) > 0)) return true;
			}
		} 
		catch (Exception e) { }
		return false;
	}
	
	protected boolean HasRoomForHopperTransfer(ItemStack stack, ItemStack dest)
	{
		if (dest == null) return true;
		if ((stack.getType() == dest.getType()) && (stack.getData().getData() == dest.getData().getData()))
		    return (dest.getMaxStackSize() > dest.getAmount());
		return false;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void InventoryMoveItemEvent(InventoryMoveItemEvent p)
    {
			Inventory       destination        = p.getDestination();
        	InventoryHolder initiator_holder   = p.getInitiator().getHolder();
        	InventoryHolder destination_holder = destination.getHolder();
        	ItemStack       transferred_item   = p.getItem();
            BlockState      dest_block 		 = null;
            if (destination_holder instanceof BlockState) dest_block = (BlockState)destination_holder;
            if ((destination_holder != null) && (dest_block != null) && 
                    (initiator_holder instanceof BlockState) && (
                      (dest_block.getType() == Material.FURNACE) || 
                      (dest_block.getType() == Material.BURNING_FURNACE) || 
                      (dest_block.getType() == Material.BREWING_STAND)
                    ))
                {
                        Location hpos = dest_block.getLocation();
                        Location fpos = ((BlockState)initiator_holder).getLocation();
                        Material dtype = ((BlockState)destination_holder).getType();
                        Material ttype = transferred_item.getType();
                        // Ce je hopper na isti ravnini kot destination, potem se gre ob strnai
                        if (hpos.getBlockY() == fpos.getBlockY()) 
                        {
                                if (((dtype == Material.FURNACE) || (dtype == Material.BURNING_FURNACE)) && (
                                         (!HasRoomForHopperTransfer(transferred_item, destination.getItem(1))) || (
                                           (ttype != Material.COAL) && 
                                           (ttype != Material.LOG) && 
                                           (ttype != Material.LOG_2) && 
                                           (ttype != Material.WOOD) && 
                                           (ttype != Material.WOOD_STEP) && 
                                           (ttype != Material.SAPLING) && 
                                           (ttype != Material.WOOD_PLATE) && 
                                           (ttype != Material.STICK) && 
                                           (ttype != Material.FENCE) && 
                                           (ttype != Material.FENCE_GATE) && 
                                           (ttype != Material.WOOD_STAIRS) && 
                                           (ttype != Material.TRAP_DOOR) && 
                                           (ttype != Material.WORKBENCH) && 
                                           (ttype != Material.CHEST) && 
                                           (ttype != Material.TRAPPED_CHEST) && 
                                           (ttype != Material.DAYLIGHT_DETECTOR) && 
                                           (ttype != Material.JUKEBOX) && 
                                           (ttype != Material.NOTE_BLOCK) && 
                                           (ttype != Material.HUGE_MUSHROOM_1) && 
                                           (ttype != Material.HUGE_MUSHROOM_2) && 
                                           (ttype != Material.BLAZE_ROD) && 
                                           (ttype != Material.COAL_BLOCK) && 
                                           (ttype != Material.LAVA_BUCKET)
                                         )
                                   )) return;
                                if ((dtype == Material.BREWING_STAND) && (
                                          ((destination.getItem(0) != null) && (destination.getItem(1) != null) && (destination.getItem(2) != null)) || 
                                          ((ttype != Material.GLASS_BOTTLE) && (ttype != Material.POTION))
                                   )) return;
                        }
                        // Drugace je verjetno na vrhu?
                        else if (((hpos.getBlockY() + 1) == fpos.getBlockY()) && (hpos.getBlockX() == fpos.getBlockX()) && 
                                        (hpos.getBlockZ() == fpos.getBlockZ()))
                        {
                                if (((dtype == Material.FURNACE) || (dtype == Material.BURNING_FURNACE)) && (!HasRoomForHopperTransfer(transferred_item, destination.getItem(0)))) return;
                                if ((dtype == Material.BREWING_STAND) && ((!HasRoomForHopperTransfer(transferred_item, destination.getItem(3))) ||    
                                         (ttype != Material.REDSTONE) && 
                                         (ttype != Material.GLOWSTONE_DUST) && 
                                         (ttype != Material.SULPHUR) && 
                                         (ttype != Material.FERMENTED_SPIDER_EYE) && 
                                         (ttype != Material.SPECKLED_MELON) && 
                                         (ttype != Material.GOLDEN_CARROT) && 
                                         (ttype != Material.MAGMA_CREAM) && 
                                         (ttype != Material.SUGAR) && 
                                         (ttype != Material.SPIDER_EYE) && 
                                         (ttype != Material.GHAST_TEAR) && 
                                         (ttype != Material.BLAZE_POWDER) && 
                                         ((ttype != Material.RAW_FISH) && (transferred_item.getData().getData() != 3)) 
                                   )) return;
                        }
                }
                else if (!isSpaceAvailable(destination, transferred_item)) return;
                this.AddEventToQueue(this.NewEventPacket(p).
                             AddData("item", this.SerializeItem(transferred_item)).
                             AddData("from", this.SerializeInventoryHolder(p.getSource().getHolder())).
                             AddData("to",   this.SerializeInventoryHolder(destination_holder)).
                             AddData("init", this.SerializeInventoryHolder(initiator_holder)));
    }

	
	@EventHandler(priority = EventPriority.MONITOR)
	public void FurnaceSmeltEvent(FurnaceSmeltEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).
							AddData("block",     this.SerializeBlock(p.getBlock())).
							AddData("result",    this.SerializeItem(p.getSource())).
							AddData("source",    this.SerializeItem(p.getResult())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void FurnaceExtractEvent(FurnaceExtractEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).
							AddData("block",     this.SerializeBlock(p.getBlock())).
							AddData("player",    this.SerializeEntity(p.getPlayer())).
							AddData("item",      p.getItemType()).
							AddData("ammount",   p.getItemAmount()).
							AddData("xp",        p.getExpToDrop()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void InventoryPickupItemEvent(InventoryPickupItemEvent p)
	{
		if (p.getInventory().getType() == InventoryType.HOPPER) return;
		this.AddEventToQueue(this.NewEventPacket(p).
							AddData("inv",  this.SerializeInventory(p.getInventory())).
							AddData("item", p.getItem()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void CraftItemEvent(CraftItemEvent p)
	{
		this.AddEventToQueue(this.NewInventoryEvent(p).
							AddData("player", this.SerializeEntity(p.getWhoClicked())).
							AddData("result", this.SerializeItem(p.getRecipe().getResult())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void InventoryOpenEvent(InventoryOpenEvent p)
	{
		this.AddEventToQueue(this.NewInventoryEvent(p).AddData("player", this.SerializeEntity(p.getPlayer())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void InventoryCloseEvent(InventoryCloseEvent p)
	{
		this.AddEventToQueue(this.NewInventoryEvent(p).AddData("player", this.SerializeEntity(p.getPlayer())));
	}
	
}
