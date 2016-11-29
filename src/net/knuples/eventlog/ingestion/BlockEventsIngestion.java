package net.knuples.eventlog.ingestion;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.knuples.eventlog.*;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.inventory.BrewingStandFuelEvent;
import org.bukkit.material.MaterialData;

public class BlockEventsIngestion extends AbstractIngestionModule {

	private static final Set<Integer> nonFluidProofBlocks = new HashSet<Integer>(Arrays.asList(27, 28, 31, 32, 37, 38, 39, 40, 50, 51, 55, 59, 66, 69, 70, 75, 76, 78, 93, 94, 104, 105, 106));
	
	public BlockEventsIngestion(EventLogPlugin plugin) {
		super(plugin);
	}

	private boolean isSurroundedByWater(Block block) 
	{
		for (final BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.WEST, BlockFace.EAST, BlockFace.SOUTH}) 
		{
			final int type = block.getRelative(face).getTypeId();
    	  	if (type == 8 || type == 9) return true;
		}
		return false;
	}
	
	protected Packet SerializeBlockEvent(BlockEvent b)
	{
		return (this.NewEventPacket(b)).AddData("block", this.SerializeBlock(b.getBlock()));
	}
	
	protected Packet SerializeBlocksList(List<Block> list)
	{
		Packet result = this.GetPacket();
		int i = 0;
		for (Block block : list)
		{
			result.AddData((new Integer(i++)).toString(), this.SerializeBlock(block));
		}
		return result;
	}
	
	protected Packet SerializeBlocksListState(List<BlockState> list)
	{
		Packet result = this.GetPacket();
		int i = 0;
		for (BlockState block : list)
		{
			result.AddData((new Integer(i++)).toString(), this.SerializeBlockState(block));
		}
		return result;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void BlockExplode(BlockExplodeEvent b)
	{
		this.AddEventToQueue(this.SerializeBlockEvent(b).
				AddData("yield",     b.getYield()).
				AddData("blocks",    this.SerializeBlocksList(b.blockList())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void BlockCanBuild(BlockCanBuildEvent b)
	{
		this.AddEventToQueue(this.SerializeBlockEvent(b).AddData("buildable", b.isBuildable()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void CauldronChange(CauldronLevelChangeEvent b)
	{
		this.AddEventToQueue(this.SerializeBlockEvent(b).
				AddData("old-level", b.getOldLevel()).
				AddData("new-level", b.getNewLevel()).
				AddData("reason",    b.getReason()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void BrewingStandFuel(BrewingStandFuelEvent b)
	{
		this.AddEventToQueue(this.SerializeBlockEvent(b).
				AddData("fuel",      this.SerializeItem(b.getFuel())).
				AddData("power",     b.getFuelPower()).
				AddData("consuming", b.isConsuming()));
	}

	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PistonExtends(BlockPistonExtendEvent b)
	{
		this.AddEventToQueue(this.SerializeBlockEvent(b).
				AddData("sticky",    b.isSticky()).
				AddData("direction", b.getDirection().toString()).
				AddData("mblocks",   this.SerializeBlocksList(b.getBlocks())));
	}

	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PistonRetracts(BlockPistonRetractEvent b)
	{
		this.AddEventToQueue(this.SerializeBlockEvent(b).
				AddData("sticky",    b.isSticky()).
				AddData("direction", b.getDirection().toString()).
				AddData("mblocks",   this.SerializeBlocksList(b.getBlocks())));
	}
		
	@EventHandler(priority = EventPriority.MONITOR)
	public void BlockMultiPlace(BlockMultiPlaceEvent b)
	{
		this.AddEventToQueue(this.SerializeBlockEvent(b).
				AddData("canBuild",  b.canBuild()).
				AddData("player",    this.SerializeEntity(b.getPlayer())).
				AddData("placed",    this.SerializeBlock(b.getBlockPlaced())).
				AddData("against",   this.SerializeBlock(b.getBlockAgainst())).
				AddData("state",     this.SerializeBlockState(b.getBlockReplacedState())).
				AddData("created",   this.SerializeBlocksListState(b.getReplacedBlockStates())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void BlockBreakEvent(BlockBreakEvent b)
	{
		this.AddEventToQueue(this.SerializeBlockEvent(b).
				AddData("xp",        b.getExpToDrop()).
				AddData("player",    this.SerializeEntity(b.getPlayer())));
	}

	
	@EventHandler(priority = EventPriority.MONITOR)
	public void BlockBurnEvent(BlockBurnEvent b)
	{
		this.AddEventToQueue(this.SerializeBlockEvent(b));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void BlockDamageEvent(BlockDamageEvent b)
	{
		this.AddEventToQueue(this.SerializeBlockEvent(b).
				AddData("player",    this.SerializeEntity(b.getPlayer())).
				AddData("instabrk",  b.getInstaBreak()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void BlockDispenseEvent(BlockDispenseEvent b)
	{
		this.AddEventToQueue(this.SerializeBlockEvent(b).
				AddData("item",      this.SerializeItem(b.getItem())));	
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void BlockExpEvent(BlockExpEvent b)
	{
		if (b.getExpToDrop() == 0) return ;
		this.AddEventToQueue(this.SerializeBlockEvent(b).
							 AddData("xp", b.getExpToDrop()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void BlockFadeEvent(BlockFadeEvent b)
	{
		this.AddEventToQueue(this.SerializeBlockEvent(b).AddData("blockto", this.SerializeBlockState(b.getNewState())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void BlockFormEvent(BlockFormEvent b)
	{
		this.AddEventToQueue(this.SerializeBlockEvent(b).AddData("blockto", this.SerializeBlockState(b.getNewState())));
	}
	
	protected boolean AddLiquidPacket(BlockFromToEvent b, int newid, byte newbyte)
	{
		BlockState newblk = b.getToBlock().getState(); newblk.setTypeId(newid); newblk.setRawData((byte) newbyte);
		AddEventToQueue(CustomEventPacket("LiquidFlow").AddData("block", this.SerializeBlock(b.getBlock())).AddData("newblock", this.SerializeBlockState(newblk)));
	    return false;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void BlockFromToEvent(BlockFromToEvent b)
	{
		if ((b.getBlock().getType() == Material.AIR) && (b.getToBlock().getType() == Material.AIR)) return;
		final Block to = b.getToBlock();
		final int typeFrom = b.getBlock().getTypeId();
		final int typeTo = to.getTypeId();
		final boolean canFlow = typeTo == 0 || nonFluidProofBlocks.contains(typeTo);
		boolean cont = true;
		if (typeFrom == 10 || typeFrom == 11) 
		{
			if (canFlow) 
			{
				if (isSurroundedByWater(to) && b.getBlock().getData() <= 2) cont = AddLiquidPacket(b, 4, (byte) 0);
				else if (typeTo == 0) cont = AddLiquidPacket(b, 10, (byte) 0);
				else cont = AddLiquidPacket(b, 10, (byte) 0);
			} 
			else if (typeTo == 8 || typeTo == 9) 
			{
				if (b.getFace() == BlockFace.DOWN) cont = AddLiquidPacket(b, 1, (byte) 0);
				else cont = AddLiquidPacket(b, 4, (byte) 0);
			}
			cont = false;
		} 
		else if (typeFrom == 8 || typeFrom == 9)
		{
			if (typeTo == 0) cont = AddLiquidPacket(b, 8, (byte) 0); 
			else if (nonFluidProofBlocks.contains(typeTo)) cont = AddLiquidPacket(b, 8, (byte) 0);
			else if (typeTo == 10 || typeTo == 11)
			{
				if (to.getData() == 0) cont = AddLiquidPacket(b, 49, (byte) 0);
				else if (b.getFace() == BlockFace.DOWN) cont = AddLiquidPacket(b, 1, (byte) 0);
			}
			if (typeTo == 0 || nonFluidProofBlocks.contains(typeTo)) 
			{
				for (final BlockFace face : new BlockFace[]{BlockFace.DOWN, BlockFace.NORTH, BlockFace.WEST, BlockFace.EAST, BlockFace.SOUTH}) 
				{
					final Block lower = to.getRelative(face);
					if (lower.getTypeId() == 10 || lower.getTypeId() == 11) cont = AddLiquidPacket(b, lower.getData() == 0 ? 49 : 4, (byte) 0);
				}
			}
			cont = false;
		}
		if (cont) 
		{
			this.AddEventToQueue(this.SerializeBlockEvent(b).AddData("newblk", this.SerializeBlock(b.getToBlock())));
		}
							 
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void BlockGrowEvent(BlockGrowEvent b)
	{
		this.AddEventToQueue(this.SerializeBlockEvent(b));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void BlockIgniteEvent(BlockIgniteEvent b)
	{
		Packet p = this.SerializeBlockEvent(b).AddData("cause", b.getCause());
		if (b.getIgnitingBlock() != null) p.AddData("iblock", this.SerializeBlock(b.getIgnitingBlock()));
		else if (b.getPlayer() != null) p.AddData("iplayer", this.SerializeEntity(b.getPlayer()));
		else if (b.getIgnitingEntity() != null) p.AddData("ientity", this.SerializeEntity(b.getIgnitingEntity()));
		this.AddEventToQueue(p);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void BlockPlaceEvent(BlockPlaceEvent b)
	{
		this.AddEventToQueue(this.SerializeBlockEvent(b).
				  			 AddData("player",   this.SerializeEntity(b.getPlayer())).
				    		 AddData("canbuild", b.canBuild()).
							 AddData("placed",   this.SerializeBlock(b.getBlockPlaced())).
							 AddData("blockag",  this.SerializeBlock(b.getBlockAgainst())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void BlockSpreadEvent(BlockSpreadEvent b)
	{
		this.AddEventToQueue(this.SerializeBlockEvent(b).
							 AddData("sourceblock", this.SerializeBlock(b.getSource())).
							 AddData("blockfrom",   this.SerializeBlockState(b.getNewState())));	
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityBlockFormEvent(EntityBlockFormEvent b)
	{
		this.AddEventToQueue(this.SerializeBlockEvent(b).
							 AddData("entity", this.SerializeEntity(b.getEntity())).
							 AddData("newblk", this.SerializeBlockState(b.getNewState())));
	}
	
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void LeavesDecayEvent(LeavesDecayEvent b)
	{
		this.AddEventToQueue(this.SerializeBlockEvent(b));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void NotePlayEvent(NotePlayEvent b)
	{
		this.AddEventToQueue(this.SerializeBlockEvent(b).
							 AddData("note",       b.getInstrument()).
							 AddData("instrument", b.getNote()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void SignChangeEvent(SignChangeEvent b)
	{
		Packet lines = this.GetPacket().AddData("line0", b.getLine(0)).
									AddData("line1", b.getLine(1)).
									AddData("line2", b.getLine(2)).
									AddData("line3", b.getLine(3));
		this.AddEventToQueue(this.SerializeBlockEvent(b).
							 AddData("lines",  lines).
							 AddData("player", this.SerializeEntity(b.getPlayer())));
	}
	
}
