package net.knuples.eventlog.ingestion;

import net.knuples.eventlog.EventLogPlugin;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveIngest extends AbstractIngestionModule {

	public PlayerMoveIngest(EventLogPlugin plugin) {
		super(plugin);
	}
	
	private boolean AreTheSameLocations(Location loc1, Location loc2)
	{
		return ((loc1.getBlockX() == loc2.getBlockX()) && (loc1.getBlockY() == loc2.getBlockY()) && 
			    (loc1.getBlockZ() == loc2.getBlockZ()) && (loc1.getWorld().getName() == loc2.getWorld().getName()));
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerMoveEvent(PlayerMoveEvent p)
	{
		try
		{
			if (AreTheSameLocations(p.getFrom(), p.getTo())) return;
			this.AddEventToQueue(this.NewEventPacket(p).
							AddData("player", this.SerializeEntity(p.getPlayer())).
							AddData("to",     this.SerializeLocation(p.getTo())));
		}
		catch (Exception e) { };
	}
	
}
