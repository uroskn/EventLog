package net.knuples.eventlog.ingestion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitScheduler;

import net.knuples.eventlog.EventLogPlugin;
import net.knuples.eventlog.StatusCounter;
import net.knuples.misc.StatHashMap;

public class ComplexIngestion extends AbstractIngestionModule {

	public StatHashMap<UUID, Entity> entitylist;
	public int shedulerid;
	
	public int missdissapear;
	public int lastrun;
	
	public int cooldown = 0;
	
	public ComplexIngestion(EventLogPlugin plugin) {
		super(plugin);
		this.BuildEntityList(0);
        shedulerid = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin, new Runnable() {
            public void run() { MissTrackEntitites(); }
        }, 100L, 100L);
	}
	
	@Override
	public void StopCollection()
	{
		Bukkit.getServer().getScheduler().cancelTask(shedulerid);
		MissTrackEntitites();
	}
	
	public void MissTrackEntitites()
	{
    	lastrun = 0;
    	cooldown--;
    	ArrayList<UUID> list = new ArrayList<UUID>();
    	for(Entry<UUID, Entity> entry : entitylist.entrySet()) 
    	{
    		if (entry.getValue().isDead()) 
    		{
    			list.add(entry.getKey());
    			missdissapear++;
    			lastrun++;
    			AddEventToQueue(CustomEventPacket("EntityRemoved").AddData("entity", SerializeEntity(entry.getValue())));
    		}
    		else if (entry.getValue() instanceof PigZombie)
    		{
    			PigZombie zombie = (PigZombie)entry.getValue();
    			if (zombie.isAngry() != zombie.getMetadata("__EL_WAS_ANGRY").get(0).asBoolean())
    			{
    				String name = "PigmanCoolDownEvent";
    				if (zombie.isAngry()) name = "PigmanAngerEvent";
    				AddEventToQueue(CustomEventPacket(name).AddData("pigmen", SerializeEntity(zombie)));
    			}
    			zombie.setMetadata("__EL_WAS_ANGRY", new FixedMetadataValue(this.plugin, zombie.isAngry()));
    		}
    	}
    	int mapsize = (entitylist.size() - list.size());
    	if (((entitylist.mapSize() / 2) < mapsize) || (cooldown > 0)) for (UUID e : list) entitylist.remove(e);
    	else BuildEntityList(entitylist.size());
	}
	
	public void BuildEntityList(int size)
	{
		int oldsize= 0;
		try
		{
			oldsize = entitylist.mapSize();
		} catch (NullPointerException e) { /* noop */ }
		if (size == 0) size = 500;
		cooldown = 10;
		this.entitylist = new StatHashMap<UUID, Entity>(size, (float)1.5);
		for (World w : this.plugin.getServer().getWorlds())
		{
			for (Chunk ch : w.getLoadedChunks())
			{
				for (Entity e : ch.getEntities()) 
				{
					entitylist.put(e.getUniqueId(), e);
					if (e instanceof PigZombie) ((PigZombie)e).setMetadata("__EL_WAS_ANGRY", new FixedMetadataValue(this.plugin, ((PigZombie)e).isAngry()));
				}
			}
		}
		this.plugin.getLogger().info("Rebuild entity list from " + oldsize + " to " + entitylist.mapSize());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void ChunkLoadEvent(ChunkLoadEvent p)
	{
		for (Entity e : p.getChunk().getEntities()) 
		{
			if (e instanceof PigZombie) ((PigZombie)e).setMetadata("__EL_WAS_ANGRY", new FixedMetadataValue(this.plugin, ((PigZombie)e).isAngry()));
			entitylist.put(e.getUniqueId(), e);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void ChunkUnloadEvent(ChunkUnloadEvent p)
	{
		for (Entity e : p.getChunk().getEntities()) 
		{
			entitylist.remove(e.getUniqueId());
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void CreatureSpawnEvent(CreatureSpawnEvent p)
	{
		if (p.getEntity() instanceof PigZombie) ((PigZombie)p.getEntity()).setMetadata("__EL_WAS_ANGRY", new FixedMetadataValue(this.plugin, ((PigZombie)p.getEntity()).isAngry()));
		this.entitylist.put(p.getEntity().getUniqueId(), p.getEntity());
	}
	
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void ItemDespawnEvent(ItemDespawnEvent p)
	{
		this.entitylist.remove(p.getEntity().getUniqueId());
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void EntityExplodeEvent(EntityExplodeEvent p)
	{
		this.entitylist.remove(p.getEntity().getUniqueId());
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void EntityDeathEvent(EntityDeathEvent p)
	{
		this.entitylist.remove(p.getEntity().getUniqueId());
	}
		
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void ItemSpawnEvent(ItemSpawnEvent p)
	{
		this.entitylist.put(p.getEntity().getUniqueId(), p.getEntity());
	}

}
