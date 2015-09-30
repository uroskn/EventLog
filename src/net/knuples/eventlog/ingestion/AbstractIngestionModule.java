package net.knuples.eventlog.ingestion;

import net.knuples.eventlog.*;

import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.block.*;
import org.bukkit.Location;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class AbstractIngestionModule implements Listener {
  protected EventLogPlugin plugin;
  
  private Packet _SerializeEntity(Entity e)
  {
	  try
	  {
		  e.getMetadata("_EV_PORTAL_EVENT").clear();
	  }
	  catch (Exception ex) { };
	  return this.GetPacket().AddData("eid",        e.getEntityId()).
			  				  AddData("type",       e.getType()).
			  				  AddData("guid",       e.getUniqueId()).
			  				  AddData("location",   this.SerializeLocation(e.getLocation()));
  }
  
  protected Packet SerializePotionEffects(Collection<PotionEffect> pot)
  {
	  try
	  {
		  Iterator<PotionEffect> po = pot.iterator();
		  Packet r = this.GetPacket();
		  int i = 0;
		  while (po.hasNext())
		  {
			  PotionEffect pe = po.next();
			  r.AddData((new Integer(i)).toString(), this.GetPacket().
					  AddData("duration",  pe.getDuration()).
					  AddData("name",      pe.getType()).
					  AddData("amplifier", pe.getAmplifier()));
			  i++;
		  }
		  return r;
	  }
	  catch (Exception e)
	  {
		  return this.GetPacket();
	  }
  }
  
  private Packet _SerializeLivingEntity(LivingEntity e)
  {
	  Packet t = this._SerializeEntity(e).AddData("isliving",  true).
			  							  AddData("firetick",  e.getFireTicks()).
			  							  AddData("air",       e.getRemainingAir()).
			  							  AddData("health",    e.getHealth()).
			  							  AddData("maxhealth", e.getMaxHealth()).
			                              AddData("custoname", e.getCustomName()).
	                                      AddData("peffects", this.SerializePotionEffects(e.getActivePotionEffects()));
	  if (e instanceof Player) this._SerializePlayer((Player)e, t);
	  if (e instanceof Ageable) t.AddData("age", ((Ageable) e).getAge()).AddData("adult", ((Ageable) e).isAdult());  
	  if (e instanceof Tameable) 
	  {
		  if (((Tameable) e).getOwner() != null) t.AddData("owner", ((Tameable) e).getOwner().getName());
		  else t.AddData("owner", "");
	  }
	  if (e instanceof PigZombie) t.AddData("angry", ((PigZombie)e).getAnger());
	  if (e instanceof Enderman) t.AddData("block", this.SerializeItem(((Enderman)e).getCarriedMaterial().toItemStack()));
	  if (e instanceof Horse)
	  {
		  t.AddData("ishorse", true).AddData("horsedata", this.GetPacket().
				  AddData("color",         ((Horse) e).getColor()).
				  AddData("jumpstrenght",  ((Horse) e).getJumpStrength()).
				  AddData("style",         ((Horse) e).getStyle()).
				  AddData("variant",       ((Horse) e).getVariant()).
				  AddData("domestication", ((Horse) e).getDomestication()).
				  AddData("getmaxdom",     ((Horse) e).getMaxDomestication()));
	  }
	  return t;
  }
  
  private void _SerializePlayer(Player p, Packet t)
  {
	  t.AddData("network", this.GetPacket().AddData("ip", p.getAddress().getHostString()).
			  									   AddData("port", p.getAddress().getPort())).
			  							   AddData("isplayer",   true).
			  							   AddData("name",       p.getDisplayName()).
			  			     			   AddData("xp",         p.getTotalExperience()).
			  							   AddData("xplevel",    p.getLevel()).
			  							   AddData("food",       p.getFoodLevel()).
			  							   AddData("gamemode",   p.getGameMode()).
			  							   AddData("isop",       p.isOp()).
			  							   AddData("saturation", p.getSaturation());
  }
  
  private Packet _SerializeItemEntity(Item i)
  {
	  return this.GetPacket().AddData("entity", this._SerializeEntity(i)).
			                  AddData("item",   this.SerializeItem(i.getItemStack()));
  }
  
  protected Packet SerializeEntity(Entity e)
  {
	  if (e == null) return this.GetPacket();
	  if (e instanceof LivingEntity) return this._SerializeLivingEntity((LivingEntity)e);
	  else if (e instanceof Item) return this._SerializeItemEntity((Item)e);
	  else return this._SerializeEntity(e);
  }
  
  protected Packet SerializeLocation(Location loc)
  {
	  return this.GetPacket().AddData("x",     loc.getBlockX()).
			                  AddData("y",     loc.getBlockY()).
			                  AddData("z",     loc.getBlockZ()).
			                  AddData("world", loc.getWorld().getName());
  }
  
  protected Packet SerializeInventoryHolder(InventoryHolder h)
  {
	  if (h == null) return this.GetPacket().AddData("unknown", true);
	  if (h instanceof BlockState) return this.SerializeBlockState((BlockState) h);
	  else if (h instanceof Entity) return this.SerializeEntity((Entity)h);
	  return this.GetPacket();
  }
  
  protected Packet SerializeBlock(Block block)
  {
	  if (block != null)
	  {
		  return this.GetPacket().AddData("id",       block.getTypeId()).
				  				  AddData("data",     block.getData()).
				  				  AddData("light",    block.getLightLevel()).
				  				  AddData("location", this.SerializeLocation(block.getLocation())).
				  				  AddData("material", block.getType().toString());
	  }
	  else return this.GetPacket();
  }
  
  protected Packet SerializeBlockState(BlockState block)
  {
	  if (block != null)
	  {
		  return this.GetPacket().AddData("id",       block.getTypeId()).
				  				  AddData("data",     block.getData()).
				  				  AddData("light",    block.getLightLevel()).
				  				  AddData("location", this.SerializeLocation(block.getLocation())).
				  				  AddData("material", block.getType());
	  }
	  else return this.GetPacket();
  }
  
  protected Packet CustomEventPacket(String name)
  {
	  return this.GetPacket().AddData("event", "EL" + name).AddData("date", System.currentTimeMillis());
  }
  
  protected Packet NewEventPacket(Event e)
  {
	  Packet p = this.GetPacket().AddData("event", e.getEventName()).AddData("date", System.currentTimeMillis());
	  if (e instanceof Cancellable) p.AddData("cancelled", ((Cancellable) e).isCancelled());
	  return p;
  }
  
  protected Packet SerializeItem(ItemStack item)
  {
	  if (item == null) return this.GetPacket();
	  return this.GetPacket().AddData("itemstr", item).
			  		  		  AddData("durability", item.getDurability()).
			  				  AddData("typeid", item.getTypeId()).
			  				  AddData("data", item.getData()).
			  				  AddData("count", item.getAmount());
  }
  
  protected Packet GetPacket()
  {
	  return new Packet();
  }
  
  public AbstractIngestionModule(EventLogPlugin plugin)
  {
	  this.plugin = plugin;
	  this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
  }
  
  public void AddEventToQueue(Packet packet)
  {
	  this.plugin.EnqueueEvent(packet);
  }
  
  public void StopCollection() { /* no-op */ };
  
}
