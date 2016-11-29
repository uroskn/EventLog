package net.knuples.eventlog.ingestion;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.*;
import org.bukkit.event.weather.*;
import org.bukkit.event.vehicle.*;
import org.bukkit.event.server.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import net.knuples.eventlog.EventLogPlugin;
import net.knuples.eventlog.Packet;

public class WorldIngest extends AbstractIngestionModule {

	public class VehicleCollisionLog
	{
		public Vehicle vehicle;
		public HashMap<Entity, Long> collisionlog;
		
		public VehicleCollisionLog(Vehicle v)
		{
			this.vehicle = v;
			this.collisionlog = new HashMap<Entity, Long>();
		}
		
		public boolean recentCollision(Entity e)
		{
			Long k = collisionlog.get(e);
			if (k == null)
			{
				collisionlog.put(e, new Long(ctick));
				return false;
			}
			boolean result = false;
			if ((ctick - k.longValue()) < 10) result = true;
			collisionlog.put(e, new Long(ctick));
			return result;
		}
	}
	
	public int   schedid1;
	public long  ctick;
	public HashMap<Vehicle, VehicleCollisionLog> vlog;
	
	public WorldIngest(EventLogPlugin plugin) {
		super(plugin);
		vlog = new HashMap<Vehicle, VehicleCollisionLog>();
		ctick = 0;
		schedid1 = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin, new Runnable() {
            public void run() { ctick++; }
        }, 1L, 1L);
	}
	
	@Override
	public void StopCollection()
	{
		Bukkit.getServer().getScheduler().cancelTask(schedid1);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void ChunkLoadEvent(ChunkLoadEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).
							AddData("world", p.getWorld().getName()).
							AddData("chunk", p.getChunk()).
							AddData("new",   p.isNewChunk()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void ChunkPopulateEvent(ChunkPopulateEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).
							AddData("world", p.getWorld().getName()).
							AddData("chunk", p.getChunk()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void ChunkUnloadEvent(ChunkUnloadEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).
							AddData("world", p.getWorld().getName()).
							AddData("chunk", p.getChunk()));
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void PortalCreateEvent(PortalCreateEvent p)
	{
		Packet pa = this.NewEventPacket(p);
		pa.AddData("reason", p.getReason());
		pa.AddData("world", p.getWorld());
		Packet blist = this.GetPacket();
		for (int i = 0; i < p.getBlocks().size(); i++)
		{
			blist.AddData((new Integer(i)).toString(), this.SerializeBlock(p.getBlocks().get(i)));
		}
		pa.AddData("blocks", blist);
		this.AddEventToQueue(pa);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void StructureGrowEvent(StructureGrowEvent p)
	{
		Packet pa = this.NewEventPacket(p);
		pa.AddData("player",  this.SerializeEntity(p.getPlayer()));
		pa.AddData("world",   p.getWorld());
		pa.AddData("species", p.getSpecies());
		Packet blist = this.GetPacket();
		for (int i = 0; i < p.getBlocks().size(); i++)
		{
			blist.AddData((new Integer(i)).toString(), this.SerializeBlock(p.getBlocks().get(i).getBlock()));
		}
		pa.AddData("blocks", blist);
		this.AddEventToQueue(pa);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void LightningStrikeEvent(LightningStrikeEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).
							AddData("world",  p.getWorld().getName()).
							AddData("strike", this.SerializeLocation(p.getLightning().getLocation())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void ThunderChangeEvent(ThunderChangeEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).
							AddData("world",  p.getWorld().getName()).
							AddData("state",  p.toThunderState()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void WeatherChangeEvent(WeatherChangeEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).
							AddData("world",  p.getWorld().getName()).
							AddData("state",  p.toWeatherState()));
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void VehicleCreateEvent(VehicleCreateEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).AddData("vehicle", this.SerializeEntity(p.getVehicle())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void VehicleBlockCollisionEvent(VehicleBlockCollisionEvent p)
	{
		Entity e = p.getVehicle();
		if (e instanceof LivingEntity) return;
		if (e.getMetadata("__EV_BLOCK_COLISION_EVENT").size() != 0)
		{
			try
			{
				Location loc = new Location(e.getWorld(), 
						e.getMetadata("__EV_BLOCK_COLISION_EVENT_X").get(0).asDouble(), 
						e.getMetadata("__EV_BLOCK_COLISION_EVENT_Y").get(0).asDouble(), 
						e.getMetadata("__EV_BLOCK_COLISION_EVENT_Z").get(0).asDouble());
				if (AreTheSameLocations(e.getLocation(), loc)) return;
			}
			catch (Exception ex) { }
		}
		e.setMetadata("__EV_BLOCK_COLISION_EVENT",   new FixedMetadataValue(this.plugin, true));
		e.setMetadata("__EV_BLOCK_COLISION_EVENT_X", new FixedMetadataValue(this.plugin, e.getLocation().getX()));
		e.setMetadata("__EV_BLOCK_COLISION_EVENT_Y", new FixedMetadataValue(this.plugin, e.getLocation().getY()));
		e.setMetadata("__EV_BLOCK_COLISION_EVENT_Z", new FixedMetadataValue(this.plugin, e.getLocation().getZ()));
		this.AddEventToQueue(this.NewEventPacket(p).
							AddData("block",   this.SerializeBlock(p.getBlock())).
							AddData("vehicle", this.SerializeEntity(e)));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void VehicleDamageEvent(VehicleDamageEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).
							AddData("attacker", this.SerializeEntity(p.getAttacker())).
							AddData("damage",   p.getDamage()).
							AddData("vehicle",  this.SerializeEntity(p.getVehicle())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void VehicleDestroyEvent(VehicleDestroyEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).
							AddData("attacker", this.SerializeEntity(p.getAttacker())).
							AddData("vehicle",  this.SerializeEntity(p.getVehicle())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void VehicleEnterEvent(VehicleEnterEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).
							AddData("entered",  this.SerializeEntity(p.getEntered())).
							AddData("vehicle",  this.SerializeEntity(p.getVehicle())));
	}
	
	private boolean AreTheSameLocations(Location loc1, Location loc2)
	{
		return ((loc1.getBlockX() == loc2.getBlockX()) && (loc1.getBlockY() == loc2.getBlockY()) && 
			    (loc1.getBlockZ() == loc2.getBlockZ()) && (loc1.getWorld().getName() == loc2.getWorld().getName()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void VehicleEntityCollisionEvent(VehicleEntityCollisionEvent p)
	{
		Vehicle e = p.getVehicle();
		if (AreTheSameLocations(p.getEntity().getLocation(), p.getVehicle().getLocation()))
		{
			if (e.hasMetadata("__EV_COLISION_EVENT_X"))
			{
				try
				{
					Location loc = new Location(e.getWorld(), 
							e.getMetadata("__EV_COLISION_EVENT_X").get(0).asDouble(), 
							e.getMetadata("__EV_COLISION_EVENT_Y").get(0).asDouble(), 
							e.getMetadata("__EV_COLISION_EVENT_Z").get(0).asDouble());
					if (AreTheSameLocations(e.getLocation(), loc)) return;
				}
				catch (Exception ex) { }
			}
			e.setMetadata("__EV_COLISION_EVENT_X", new FixedMetadataValue(this.plugin, e.getLocation().getX()));
			e.setMetadata("__EV_COLISION_EVENT_Y", new FixedMetadataValue(this.plugin, e.getLocation().getY()));
			e.setMetadata("__EV_COLISION_EVENT_Z", new FixedMetadataValue(this.plugin, e.getLocation().getZ()));
		}
		VehicleCollisionLog log = vlog.get(e);
		if (log == null) 
		{
			log = new VehicleCollisionLog(e);
			vlog.put(e, log);
		}
		if (log.recentCollision(p.getEntity())) return;
		this.AddEventToQueue(this.NewEventPacket(p).
							AddData("entity",   this.SerializeEntity(p.getEntity())).
							AddData("vehicle",  this.SerializeEntity(p.getVehicle())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void VehicleExitEvent(VehicleExitEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).
							AddData("exited",   this.SerializeEntity(p.getExited())).
							AddData("vehicle",  this.SerializeEntity(p.getVehicle())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void VehicleMoveEvent(VehicleMoveEvent p)
	{
		try
		{
			if (AreTheSameLocations(p.getFrom(), p.getTo())) return;
		    this.AddEventToQueue(this.NewEventPacket(p).
							     AddData("from",     this.SerializeLocation(p.getFrom())).
							     AddData("vehicle",  this.SerializeEntity(p.getVehicle())));
		}
		catch (Exception e) { }
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PluginDisableEvent(PluginDisableEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).AddData("plugin", p.getPlugin().getName()));
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void PluginEnableEvent(PluginEnableEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).AddData("plugin", p.getPlugin().getName()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void ServerListPingEvent(ServerListPingEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).
							AddData("address",  p.getAddress()).
							AddData("motd",     p.getMotd()).
							AddData("curpl",    p.getNumPlayers()).
							AddData("maxpl",    p.getMaxPlayers()));
	}
	
}
