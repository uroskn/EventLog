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
import org.bukkit.material.Colorable;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class AbstractIngestionModule implements Listener {
  protected EventLogPlugin plugin;
  protected HashSet<Entity> loop_check;
  
  private Packet _SerializeEntity(Entity e)
  {
	  try
	  {
		  e.getMetadata("_EV_PORTAL_EVENT").clear();
	  }
	  catch (Exception ex) { };
	  Packet p = this.GetPacket().AddData("eid",         e.getEntityId()).
			  				  AddData("type",        e.getType()).
			  				  AddData("guid",        e.getUniqueId()).
			  				  AddData("falldist",    e.getFallDistance()).
			  				  AddData("passenger",   this.SerializeEntity(e.getPassenger())).
			  				  AddData("portal-cool", e.getPortalCooldown()).
			  				  AddData("ground",      e.isOnGround()).
			  				  AddData("location",    this.SerializeLocation(e.getLocation()));
	  if (e instanceof Damageable)
	  {
		  p.AddData("health",     ((Damageable) e).getHealth()).
			  AddData("maxhealth",  ((Damageable) e).getMaxHealth());
	  }
	  if (e instanceof Vehicle) p.AddData("velocity", e.getVelocity());
	  if (e instanceof Colorable) p.AddData("color", ((Colorable) e).getColor());
	  return p;

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
  
  private Packet _SerializeLivingSpecifics(LivingEntity e, Packet t)
  {
	  if (e instanceof Player) this._SerializePlayer((Player)e, t);
	  else if (e instanceof Bat)         t.AddData("awake", ((Bat) e).isAwake());
	  else if (e instanceof EnderDragon) t.AddData("phase", ((EnderDragon) e).getPhase());
	  else if (e instanceof Villager)
	  {
		  t.AddData("profession", ((Villager) e).getProfession()).
		    AddData("riches",     ((Villager) e).getRiches()).
		    AddData("recipes",    ((Villager) e).getRecipeCount()).
		    AddData("trading",    ((Villager) e).getTrader());
	  }
	  else if (e instanceof Animals)
	  {
		  if (e instanceof AbstractHorse)
		  {
			  t.AddData("ishorse", true).AddData("horsedata", this.GetPacket().
					  AddData("jumpstrenght",  ((AbstractHorse) e).getJumpStrength()).
					  AddData("variant",       ((AbstractHorse) e).getVariant()).
					  AddData("domestication", ((AbstractHorse) e).getDomestication()).
					  AddData("getmaxdom",     ((AbstractHorse) e).getMaxDomestication()));
			  if (e instanceof ChestedHorse) t.AddData("chest", ((ChestedHorse) e).isCarryingChest());
			  else if (e instanceof Horse)
			  {
				  t.AddData("color",    ((Horse) e).getColor()).
				   AddData("style",    ((Horse) e).getStyle()).
				   AddData("invetory", this.GetPacket().
						  AddData("armor",   this.SerializeItem(((Horse) e).getInventory().getArmor())).
						  AddData("saddle",  this.SerializeItem(((Horse) e).getInventory().getSaddle())));
			  }
			  else if (e instanceof Llama)
			  {
				  t.AddData("color",    ((Llama) e).getColor()).
				    AddData("strenght", ((Llama) e).getStrength()).
				    AddData("decor",    this.SerializeItem(((Llama) e).getInventory().getDecor()));
			  }
		  }
		  else if (e instanceof Ocelot)
		  {
			  t.AddData("type",    ((Ocelot) e).getCatType()).
			  	AddData("sitting", ((Ocelot) e).isSitting());
		  }
		  else if (e instanceof Wolf)
		  {
			  t.AddData("collar-color", ((Wolf) e).getCollarColor()).
			    AddData("angry",        ((Wolf) e).isAngry()).
			    AddData("sitting",      ((Wolf) e).isSitting());
		  }
		  else if (e instanceof Pig) t.AddData("saddle", ((Pig) e).hasSaddle());
		  else if (e instanceof Rabbit) t.AddData("type", ((Rabbit) e).getRabbitType());
		  else if (e instanceof Sheep) t.AddData("sheared", ((Sheep) e).isSheared());
	  }
	  else if (e instanceof Monster)
	  {
		  if (e instanceof Zombie)
		  {
			  t.AddData("baby", ((Zombie) e).isBaby());
			  if (e instanceof PigZombie) 
			  {
				  t.AddData("level", ((PigZombie)e).getAnger()).
				    AddData("angry", ((PigZombie) e).isAngry());
			  }
			  else if (e instanceof ZombieVillager)
			  {
				  t.AddData("profession", ((ZombieVillager) e).getVillagerProfession());
			  }
		  }
		  else if (e instanceof Enderman)  t.AddData("block", this.SerializeItem(((Enderman)e).getCarriedMaterial().toItemStack()));
		  else if (e instanceof Creeper) t.AddData("powered", ((Creeper) e).isPowered());
		  else if (e instanceof ElderGuardian) t.AddData("elder", ((ElderGuardian) e).isElder());
		  else if (e instanceof Evoker) t.AddData("spell", ((Evoker) e).getCurrentSpell());
	  }
	  return t;
  }
  
  private Packet _SerializeLivingEntity(LivingEntity e)
  {
	  Packet t = this._SerializeEntity(e).AddData("isliving",   true).
			  							  AddData("firetick",   e.getFireTicks()).
			  							  AddData("air",        e.getRemainingAir()).
			  							  AddData("maxair",     e.getMaximumAir()).
			                              AddData("customname", e.getCustomName()).
			                              AddData("passenger",  this.SerializeEntity(e.getPassenger())).
			                              AddData("nodmgticks", e.getNoDamageTicks()).
			                              AddData("has-ai",     e.hasAI()).
			                              AddData("gravity",    e.hasGravity()).
			                              AddData("can-pickup", e.getCanPickupItems()).
			                              AddData("gliding",    e.isGliding()).
			                              AddData("collidable", e.isCollidable()).
	                                      AddData("peffects",   this.SerializePotionEffects(e.getActivePotionEffects()));
	  if (e.isLeashed()) t.AddData("leash", this.SerializeEntity(e.getLeashHolder()));
	  if (e instanceof Creature)
	  {
		  Creature cr = (Creature)e;
		  if (cr.getTarget() != null)
		  {
			  if (!loop_check.contains(cr.getTarget()))
			  {
				  loop_check.add(cr.getTarget());
				  t.AddData("target", this.SerializeEntity(cr.getTarget()));
			  }
			  else t.AddData("target", "***** LOOP *****");
		  }
		  else t.AddData("target", null);
	  }
	  if (e instanceof Ageable) 
	  {
		  t.AddData("age",   ((Ageable) e).getAge()).
		    AddData("adult", ((Ageable) e).isAdult()).
		    AddData("breed", ((Ageable) e).canBreed());  
	  }
	  if (e instanceof Tameable) 
	  {
		  if (((Tameable) e).getOwner() != null) 
		  {
			  t.AddData("owner", this.GetPacket().
					  AddData("name", ((Tameable) e).getOwner().getName()).
					  AddData("uuid", ((Tameable) e).getOwner().getUniqueId()));
		  }
		  else t.AddData("owner", "");
	  }
	  return this._SerializeLivingSpecifics(e, t);
  }
  
  private void _SerializePlayer(Player p, Packet t)
  {
	  t.AddData("network", this.GetPacket().
	      AddData("ip",         p.getAddress().getHostString()).
		  AddData("port",       p.getAddress().getPort())).
		  AddData("admin-mode", this.GetPacket().
		  AddData("gamemode",   p.getGameMode()).
		  AddData("isop",       p.isOp())).
		  AddData("isplayer",   true).
		  AddData("name",       p.getDisplayName()).
          AddData("xp",         p.getTotalExperience()).
		  AddData("xplevel",    p.getLevel()).
		  AddData("food",       p.getFoodLevel()).
		  AddData("item",       this.SerializeItem(p.getInventory().getItemInMainHand())).
		  AddData("offitem",    this.SerializeItem(p.getInventory().getItemInOffHand())).
		  AddData("armor",      this.GetPacket().
		  AddData("helmet",     this.SerializeItem(p.getInventory().getHelmet())).
		  AddData("chestplate", this.SerializeItem(p.getInventory().getChestplate())).
		  AddData("leggings",   this.SerializeItem(p.getInventory().getLeggings())).
		  AddData("boots",      this.SerializeItem(p.getInventory().getBoots()))).
		  AddData("exaustion",  p.getExhaustion()).
		  AddData("state",      this.GetPacket().
		  AddData("flying",     p.isFlying()).
		  AddData("sneaking",   p.isSneaking()).
		  AddData("sprinting",  p.isSprinting()).
		  AddData("sleeping",   p.isSleeping()).
		  AddData("blocking",   p.isBlocking())).
		  AddData("listname",   p.getPlayerListName()).
		  AddData("weather",    p.getPlayerWeather()).
		  AddData("spectating", this.SerializeEntity(p.getSpectatorTarget())).
		  AddData("spawn",      this.SerializeLocation(p.getCompassTarget())).
		  AddData("saturation", p.getSaturation());
  }
  
  private Packet _SerializeItemEntity(Item i)
  {
	  return this.GetPacket().AddData("entity", this._SerializeEntity(i)).
			                  AddData("item",   this.SerializeItem(i.getItemStack()));
  }
  
  private Packet _SerializeArrow(Arrow a)
  {
	  Packet p = this.GetPacket().AddData("entity",    this._SerializeEntity(a)).
			                      AddData("critical",  a.isCritical()).
			                      AddData("knockback", a.getKnockbackStrength());
	  if (a instanceof SpectralArrow) p.AddData("glowing", ((SpectralArrow) a).getGlowingTicks());
	  else if (a instanceof TippedArrow) p.AddData("effects", this.SerializePotionEffects(((TippedArrow) a).getCustomEffects()));
	  return p;
  }
  
  protected Packet SerializeEntity(Entity e, boolean reset_hash)
  {
	  if (reset_hash) this.loop_check = new HashSet<Entity>();
	  if (e == null) return this.GetPacket();
	  if (e instanceof LivingEntity) return this._SerializeLivingEntity((LivingEntity)e);
	  if (e instanceof Item) return this._SerializeItemEntity((Item)e);
	  if (e instanceof Arrow) return this._SerializeArrow((Arrow) e);
	  return this._SerializeEntity(e);
  }
  
  protected Packet SerializeEntity(Entity e)
  {
	  return SerializeEntity(e, true);
  }
  
  protected Packet SerializeLocation(Location loc)
  {
	  if (loc == null) return this.GetPacket();
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
