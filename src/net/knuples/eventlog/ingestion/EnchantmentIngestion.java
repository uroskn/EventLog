package net.knuples.eventlog.ingestion;

import net.knuples.eventlog.EventLogPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.enchantment.*;;

public class EnchantmentIngestion extends AbstractIngestionModule {

	public EnchantmentIngestion(EventLogPlugin plugin) {
		super(plugin);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void PrepareItemEnchantEvent(PrepareItemEnchantEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).
				AddData("player",       this.SerializeEntity(p.getEnchanter())).
				AddData("item",         this.SerializeItem(p.getItem())).
				AddData("enchantblock", this.SerializeBlock(p.getEnchantBlock())).
				AddData("offers",       p.getExpLevelCostsOffered()).
				AddData("enchtbonus",   p.getEnchantmentBonus()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void EnchantItemEvent(EnchantItemEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).
				AddData("player",       this.SerializeEntity(p.getEnchanter())).
				AddData("item",         this.SerializeItem(p.getItem())).
				AddData("enchantblock", this.SerializeBlock(p.getEnchantBlock())).
				AddData("enchants",     p.getEnchantsToAdd()).
				AddData("cost",         p.getExpLevelCost()));
	}
	
}

