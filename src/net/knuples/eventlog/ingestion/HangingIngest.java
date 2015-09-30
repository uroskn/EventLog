package net.knuples.eventlog.ingestion;

import net.knuples.eventlog.EventLogPlugin;
import net.knuples.eventlog.Packet;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.hanging.*;

public class HangingIngest extends AbstractIngestionModule {

	public HangingIngest(EventLogPlugin plugin) {
		super(plugin);
		// TODO Auto-generated constructor stub
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void HangingPlaceEvent(HangingPlaceEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).
							AddData("entity",    this.SerializeEntity(p.getEntity())).
							AddData("block",     this.SerializeBlock(p.getBlock())).
							AddData("player",    this.SerializeEntity(p.getPlayer())).
							AddData("blockface", p.getBlockFace()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void HangingBreakEvent(HangingBreakEvent p)
	{
		Packet pa = this.NewEventPacket(p).AddData("cause", p.getCause());
		if (p instanceof HangingBreakByEntityEvent) pa.AddData("entity", this.SerializeEntity(p.getEntity()));
		this.AddEventToQueue(pa);
	}

}
