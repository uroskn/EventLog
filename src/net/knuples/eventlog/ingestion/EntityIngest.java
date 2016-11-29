package net.knuples.eventlog.ingestion;

import java.util.Collection;
import java.util.List;

import net.knuples.eventlog.*;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;

public class EntityIngest extends AbstractIngestionModule {

	public EntityIngest(EventLogPlugin plugin) {
		super(plugin);
	}
	
	private Packet NewEntityEvent(EntityEvent e)
	{
		return this.NewEventPacket(e).AddData("entity", this.SerializeEntity(e.getEntity()));
	}
	
	private Packet SerializeRecipeList(MerchantRecipe m)
	{
		Packet t = this.GetPacket();
		t.AddData("uses",     m.getUses());
		t.AddData("max-uses", m.getMaxUses());
		t.AddData("result",   this.SerializeItem(m.getResult()));
		Packet temp = this.GetPacket();
		int i = 0;
		for (ItemStack itm : m.getIngredients())
	      temp.AddData((new Integer(i++)).toString(), this.SerializeItem(itm));
		return t;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void CreatureSpawnEvent(CreatureSpawnEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).
				AddData("reason",   p.getSpawnReason()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityAirChangeEvent(EntityAirChangeEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).
				AddData("ammount",   p.getAmount()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void Resurrection(EntityResurrectEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void SpawnerSpawnEvent(SpawnerSpawnEvent b)
	{
		this.AddEventToQueue(this.NewEntityEvent(b).
				AddData("block", this.SerializeBlockState(b.getSpawner()).
						AddData("ctype", b.getSpawner().getSpawnedType()).
						AddData("delay", b.getSpawner().getDelay())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void BreedEvent(EntityBreedEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).
				AddData("item",   this.SerializeItem(p.getBredWith())).
				AddData("breeder", this.SerializeEntity(p.getBreeder())).
				AddData("father",  this.SerializeEntity(p.getFather())).
				AddData("mother",  this.SerializeEntity(p.getMother())).
				AddData("xp",      p.getExperience()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void EnderDragonChangePhase(EnderDragonChangePhaseEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).
				AddData("current", p.getCurrentPhase()).
				AddData("new",     p.getNewPhase()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void CreeperPowerEvent(CreeperPowerEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).
				AddData("reason",   p.getCause()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityBreakDoorEvent(EntityBreakDoorEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).
				AddData("block",    this.SerializeBlock(p.getBlock()).
				AddData("changeto", p.getTo())));
	}
	

	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityChangeBlockEvent(EntityChangeBlockEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).
				AddData("block",    this.SerializeBlock(p.getBlock()).
				AddData("changeto", p.getTo())));
	}	
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityCombustByBlockEvent(EntityCombustByBlockEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).
				AddData("block",    this.SerializeBlock(p.getCombuster()).
				AddData("duration", p.getDuration())));
	}	
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityCombustByEntityEvent(EntityCombustByEntityEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).
				AddData("entity",   this.SerializeEntity(p.getCombuster()).
				AddData("duration", p.getDuration())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityCreatePortalEvent(EntityCreatePortalEvent p)
	{
		Packet pa = this.NewEntityEvent(p).AddData("type", p.getPortalType());
		Packet blist = this.GetPacket();
		for (int i = 0; i < p.getBlocks().size(); i++)
		{
			blist.AddData((new Integer(i)).toString(), this.SerializeBlock(p.getBlocks().get(i).getBlock()));
		}
		pa.AddData("blocks", blist);
		this.AddEventToQueue(pa);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityDamageEvent(EntityDamageEvent p)
	{
		Packet pa = this.NewEntityEvent(p).AddData("damage", p.getDamage()).AddData("cause", p.getCause());
		if (p instanceof EntityDamageByEntityEvent) pa.AddData("damager", this.SerializeEntity(((EntityDamageByEntityEvent) p).getDamager()));
		else if (p instanceof EntityDamageByBlockEvent) pa.AddData("damager", this.SerializeBlock(((EntityDamageByBlockEvent) p).getDamager()));
		this.AddEventToQueue(pa);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityDeathEvent(EntityDeathEvent p)
	{
		Packet pa = this.NewEntityEvent(p).AddData("xp", p.getDroppedExp());
		List<ItemStack> l = p.getDrops();
		Packet items = this.GetPacket();
		for (int i = 0; i < l.size(); i++) items.AddData((new Integer(i)).toString(), this.SerializeItem(l.get(i)));
		pa.AddData("dropped-items", items);
		if (p instanceof PlayerDeathEvent) pa.AddData("reason", ((PlayerDeathEvent) p).getDeathMessage());
		this.AddEventToQueue(pa);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityExplodeEvent(EntityExplodeEvent p)
	{
		Packet pa = this.NewEntityEvent(p).AddData("yield", p.getYield());
		List<Block> l = p.blockList();
		Packet items = this.GetPacket();
		for (int i = 0; i < l.size(); i++) items.AddData((new Integer(i)).toString(), this.SerializeBlock(l.get(i)));
		pa.AddData("destroyed-blocks", items);
		this.AddEventToQueue(pa);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityInteractEvent(EntityInteractEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).AddData("block", this.SerializeBlock(p.getBlock())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void FireworkExplodeEvent(FireworkExplodeEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void ItemMergeEvent(ItemMergeEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).AddData("target", this.SerializeEntity(p.getTarget())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityToggleGlideEvent(EntityToggleGlideEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).AddData("gliding", p.isGliding()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityPortalEnterEvent(EntityPortalEnterEvent p)
	{
		if (p.getEntity().getMetadata("_EV_PORTAL_EVENT").size() == 0)
		{
			this.AddEventToQueue(this.NewEntityEvent(p));
			p.getEntity().setMetadata("_EV_PORTAL_EVENT", new FixedMetadataValue(this.plugin, true));
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityPortalEvent(EntityPortalEvent p)
	{
		Packet pa = this.NewEntityEvent(p).AddData("to", this.SerializeLocation(p.getTo()));
		pa.AddData("travel-agent", (this.GetPacket()).
				AddData("cancreateportal", ((EntityPortalEvent)p).getPortalTravelAgent().getCanCreatePortal()).
				AddData("creationradius",  ((EntityPortalEvent)p).getPortalTravelAgent().getCreationRadius()).
				AddData("searchraduius",   ((EntityPortalEvent)p).getPortalTravelAgent().getSearchRadius()));
		this.AddEventToQueue(pa);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityPortalExitEvent(EntityPortalExitEvent p)
	{
		Packet pa = this.NewEntityEvent(p).AddData("to", this.SerializeLocation(p.getTo()));
		pa.AddData("vector-after",  ((EntityPortalExitEvent) p).getAfter());
		pa.AddData("vector-before", ((EntityPortalExitEvent) p).getBefore());
		this.AddEventToQueue(pa);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityTeleportEvent(EntityTeleportEvent p)
	{
		Packet pa = this.NewEntityEvent(p).AddData("from", this.SerializeLocation(p.getFrom()));
		this.AddEventToQueue(pa);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityRegainHealthEvent(EntityRegainHealthEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).
				AddData("ammount", p.getAmount()).
				AddData("reason",  p.getRegainReason()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityShootBowEvent(EntityShootBowEvent p)
	{
		Packet pa = this.NewEntityEvent(p).AddData("force", p.getForce()).AddData("projectile", this.SerializeEntity(p.getProjectile()));
		if (p.getBow() != null) pa.AddData("bow", this.SerializeItem(p.getBow()));
		this.AddEventToQueue(pa);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityTameEvent(EntityTameEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).AddData("owner", p.getOwner().getName()));
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityTargetEvent(EntityTargetEvent p)
	{
		if (p.getReason() == TargetReason.FORGOT_TARGET) p.getEntity().removeMetadata("__EV_TARGET", this.plugin);
		else
		{
		  List<MetadataValue> list = p.getEntity().getMetadata("__EV_TARGET");
		  String uuid = ""; 
		  if (p.getTarget() != null) p.getTarget().getUniqueId().toString();
		  try
		  {
			  if ((list.size() == 0) || (list.get(0).asString().compareTo(uuid) != 0))
		      {
				  p.getEntity().setMetadata("__EV_TARGET", new FixedMetadataValue(this.plugin, uuid));
		      }
			  else return;
		  }
		  catch (Exception e)
		  {
			  this.plugin.getLogger().warning("METADATA EXCEPTION: " + p.getEntity().getUniqueId().toString());
			  p.getEntity().setMetadata("__EV_TARGET", new FixedMetadataValue(this.plugin, uuid));
		  }
		}
		this.AddEventToQueue(this.NewEntityEvent(p).
				AddData("target", this.SerializeEntity(p.getTarget())).
				AddData("reason", p.getReason()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void FoodLevelChangeEvent(FoodLevelChangeEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).AddData("foodlvl", p.getFoodLevel()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void ExplosionPrimeEvent(ExplosionPrimeEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).
				AddData("fire",   p.getFire()).
				AddData("radius", (new Float(p.getRadius()))));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void ItemDespawnEvent(ItemDespawnEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void ItemSpawnEvent(ItemSpawnEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PigZapEvent(PigZapEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).
				AddData("replace-entity", this.SerializeEntity(p.getPigZombie())).
				AddData("lightning",      this.SerializeEntity(p.getLightning())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void ProjectileHitEvent(PotionSplashEvent p)
	{
		Packet pa = this.NewEntityEvent(p);
		ProjectileSource shooter = p.getPotion().getShooter();
		if (shooter instanceof LivingEntity) pa.AddData("shooter", this.SerializeEntity((Entity) shooter));
		else if (shooter instanceof BlockProjectileSource) pa.AddData("shooter", this.SerializeBlock((Block) shooter));
		Packet pot = this.GetPacket();
		pot.AddData("effects", this.SerializePotionEffects(((PotionSplashEvent) p).getPotion().getEffects()));
		pot.AddData("item",    this.SerializeItem(((PotionSplashEvent) p).getPotion().getItem()));
		pa.AddData("potion", pot);
		Collection<LivingEntity> le = ((PotionSplashEvent) p).getAffectedEntities();
		Packet affectedent = this.GetPacket();
		int i = 0;
		for (LivingEntity entity : le)
		{
			Packet affected = this.GetPacket();
			affected.AddData("intensity", p.getIntensity(entity));
			affected.AddData("entity", this.SerializeEntity(entity));
			affectedent.AddData(new Integer(i).toString(), affected);
		    i++;
		}
		pa.AddData("affected", affectedent);
		this.AddEventToQueue(pa);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void ProjectileHitEvent(ExpBottleEvent p)
	{
		Packet pa = this.NewEntityEvent(p);
		ProjectileSource shooter = p.getEntity().getShooter();
		if (shooter instanceof LivingEntity) pa.AddData("shooter", this.SerializeEntity((Entity) shooter));
		else if (shooter instanceof BlockProjectileSource) pa.AddData("shooter", this.SerializeBlock((Block) shooter));
		pa.AddData("experience", ((ExpBottleEvent) p).getExperience());
		pa.AddData("showeffect", ((ExpBottleEvent) p).getShowEffect());
		this.AddEventToQueue(pa);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void ProjectileHitEvent(ProjectileHitEvent p)
	{
		if (p.getEntityType() == EntityType.SPLASH_POTION) return;
		Packet pa = this.NewEntityEvent(p);
		ProjectileSource shooter = p.getEntity().getShooter();
		if (shooter instanceof LivingEntity) pa.AddData("shooter", this.SerializeEntity((Entity) shooter));
		else if (shooter instanceof BlockProjectileSource)  pa.AddData("shooter", this.SerializeBlock(((BlockProjectileSource) shooter).getBlock()));
		this.AddEventToQueue(pa);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void ProjectileLaunchEvent(ProjectileLaunchEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void SheepRegrowWoolEvent(SheepRegrowWoolEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void SlimeSplitEvent(SlimeSplitEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).AddData("count", p.getCount()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void SheepDyeWoolEvent(SheepDyeWoolEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).AddData("color", p.getColor()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void HorseJumpEvent(HorseJumpEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).AddData("power", p.getPower()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerLeashEntityEvent(EntityUnleashEvent p)
	{
		Packet pa = this.NewEventPacket(p).
				AddData("entity", this.SerializeEntity(p.getEntity()).
				AddData("reason", p.getReason().toString()));
		if (p instanceof PlayerUnleashEntityEvent)
			pa.AddData("player", this.SerializeEntity(((PlayerUnleashEntityEvent) p).getPlayer()));
		this.AddEventToQueue(pa);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerLeashEntityEvent(PlayerLeashEntityEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).AddData("entity", this.SerializeEntity(p.getEntity())).AddData("player", this.SerializeEntity(p.getPlayer())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void VillagerReplenishTradeEvent(VillagerReplenishTradeEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).
				AddData("bonus",  p.getBonus()).
				AddData("recipe", this.SerializeRecipeList(p.getRecipe())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void VillagerAcquireTradeEvent(VillagerAcquireTradeEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).
				AddData("recipe", this.SerializeRecipeList(p.getRecipe())));
	}
	
	/*
	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityCreatePortalEvent(EntityCreatePortalEvent p)
	{
		this.AddEventToQueue(this.NewEntityEvent(p).
				AddData("", p).
				AddData("", p));
	}*/
	
}
