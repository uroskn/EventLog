package net.knuples.eventlog.ingestion;

import java.util.Iterator;

import net.knuples.eventlog.EventLogPlugin;
import net.knuples.eventlog.Packet;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.meta.BookMeta;

public class PlayerIngest extends AbstractIngestionModule {

	public PlayerIngest(EventLogPlugin plugin) {
		super(plugin);
	}
	
	private Packet NewPlayerEvenet(PlayerEvent p)
	{
		return this.NewEventPacket(p).AddData("player", this.SerializeEntity(p.getPlayer()));
	}
	
	private Packet SerializeBookMeta(BookMeta m)
	{
		Packet p = this.GetPacket();
		p.AddData("author", m.getAuthor());
		p.AddData("title",  m.getTitle());
		p.AddData("pages",  m.getPageCount());
		Packet pages = this.GetPacket();
		for (int i = 0; i < m.getPages().size(); i++)
		{
			pages.AddData((new Integer(i)).toString(), m.getPage(i).replace('\n', ' '));
		}
		p.AddData("pages", pages);
		return p;	
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerEditBookEvent(PlayerEditBookEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).
				AddData("new_meta", this.SerializeBookMeta(p.getNewBookMeta())).
				AddData("old_meta", this.SerializeBookMeta(p.getPreviousBookMeta())).
				AddData("slot",     p.getSlot()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerBedEnterEvent(PlayerBedEnterEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).AddData("bed", this.SerializeBlock(p.getBed())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerBedLeaveEvent(PlayerBedLeaveEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).AddData("bed", this.SerializeBlock(p.getBed())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerBucketEmptyEvent(PlayerBucketEmptyEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).
							AddData("blockface", p.getBlockFace()).
							AddData("itemstack", this.SerializeItem(p.getItemStack())).
							AddData("block",     this.SerializeBlock(p.getBlockClicked())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerBucketFillEvent(PlayerBucketFillEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).
							AddData("blockface", p.getBlockFace()).
							AddData("itemstack", this.SerializeItem(p.getItemStack())).
							AddData("block",     this.SerializeBlock(p.getBlockClicked())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerChangedWorldEvent(PlayerChangedWorldEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).
				AddData("world",   p.getFrom()).
				AddData("current", p.getPlayer().getLocation().getWorld()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void AsyncPlayerChatEvent(AsyncPlayerChatEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).
				AddData("format",   p.getFormat()).
				AddData("message",  p.getMessage()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void AsyncPlayerPreLoginEvent(AsyncPlayerPreLoginEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).
				AddData("address",  p.getAddress()).
				AddData("result",   p.getLoginResult()).
				AddData("name",     p.getName()).
				AddData("message",  p.getKickMessage()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerCommandPreprocessEvent(PlayerCommandPreprocessEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).AddData("message", p.getMessage()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerDropItemEvent(PlayerDropItemEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).AddData("item", this.SerializeItem(p.getItemDrop().getItemStack())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerEggThrowEvent(PlayerEggThrowEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).
							AddData("hatching",   p.getHatchingType()).
							AddData("numhatches", p.getNumHatches()).
							AddData("ishatching", p.isHatching()).
							AddData("egg",        this.SerializeEntity(p.getEgg())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerExpChangeEvent(PlayerExpChangeEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).AddData("xp", p.getAmount()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerFishEvent(PlayerFishEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).
							AddData("xp",     p.getExpToDrop()).
							AddData("state",  p.getState()).
							AddData("hook",   this.SerializeEntity(p.getHook())).
							AddData("caught", this.SerializeEntity(p.getCaught())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerGameModeChangeEvent(PlayerGameModeChangeEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).AddData("gamemode", p.getNewGameMode()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerInteractEntityEvent(PlayerInteractEntityEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).AddData("entity", this.SerializeEntity(p.getRightClicked())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerInteractEvent(PlayerInteractEvent p)
	{
	    Block clicked = p.getClickedBlock();
		if (clicked == null) return;
	    boolean cont = false;
		switch (clicked.getType()) {
			case LEVER:
			case WOOD_BUTTON:
			case STONE_BUTTON:
			case FENCE_GATE:
			case WOODEN_DOOR:
			case TRAP_DOOR:
			case NOTE_BLOCK:
			case DIODE_BLOCK_OFF:
			case DIODE_BLOCK_ON:
			case REDSTONE_COMPARATOR_OFF:
			case REDSTONE_COMPARATOR_ON:
				if (p.getAction() == Action.RIGHT_CLICK_BLOCK) cont = true;
				break;
			case CAKE_BLOCK:
				if (p.getAction() == Action.RIGHT_CLICK_BLOCK && p.getPlayer().getFoodLevel() < 20) cont = true;
			break;
			case WOOD_PLATE:
			case STONE_PLATE:
			case IRON_PLATE:
			case GOLD_PLATE:
			case SOIL: 
				if (p.getAction() == Action.PHYSICAL) cont = true; 
			break;
		}
		if (!cont) return;
		this.AddEventToQueue(this.NewPlayerEvenet(p).
							AddData("blockface",  p.getBlockFace()).
							AddData("block",      this.SerializeBlock(p.getClickedBlock())).
							AddData("item",       this.SerializeItem(p.getItem())).
							AddData("material",   p.getMaterial()).
							AddData("hasitem",    p.hasItem()).
							AddData("hasblock",   p.hasBlock()).
							AddData("isblockhnd", p.isBlockInHand()).
							AddData("action",     p.getAction()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerItemBreakEvent(PlayerItemBreakEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).AddData("entity", this.SerializeItem(p.getBrokenItem())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerItemConsumeEvent(PlayerItemConsumeEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).AddData("entity", this.SerializeItem(p.getItem())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerItemHeldEvent(PlayerItemHeldEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).
							AddData("prevslot",     p.getPreviousSlot()).
							AddData("curslot",      p.getNewSlot()).
							AddData("selecteditem", this.SerializeItem(p.getPlayer().getInventory().getItem(p.getNewSlot()))));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerJoinEvent(PlayerJoinEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).AddData("message", p.getJoinMessage()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerKickEvent(PlayerKickEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).
							AddData("leave", p.getLeaveMessage()).
							AddData("reson", p.getReason()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerLevelChangeEvent(PlayerLevelChangeEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).
							AddData("prevlevel", p.getOldLevel()).
							AddData("newlevel",  p.getNewLevel()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerLoginEvent(PlayerLoginEvent p)
	{
		this.AddEventToQueue(this.NewEventPacket(p).
				AddData("address",  p.getAddress()).
				AddData("result",   p.getResult()).
				AddData("host",     p.getHostname()).
				AddData("message",  p.getKickMessage()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerPickupItemEvent(PlayerPickupItemEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).
							AddData("item",     this.SerializeItem(p.getItem().getItemStack())).
							AddData("leftover", p.getRemaining()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerQuitEvent(PlayerQuitEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).AddData("message", p.getQuitMessage()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerRespawnEvent(PlayerRespawnEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).
							AddData("location", this.SerializeLocation(p.getRespawnLocation())).
							AddData("isbed",    p.isBedSpawn()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerShearEntityEvent(PlayerShearEntityEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).AddData("entity", this.SerializeEntity(p.getEntity())));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerTeleportEvent(PlayerTeleportEvent p)
	{
		if (p.getFrom().equals(p.getTo())) return;
		this.AddEventToQueue(this.NewPlayerEvenet(p).
							AddData("to",    this.SerializeLocation(p.getTo())).
							AddData("cause", p.getCause()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerToggleFlightEvent(PlayerToggleFlightEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).AddData("enabled", p.isFlying()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerToggleSneakEvent(PlayerToggleSneakEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).AddData("enabled", p.isSneaking()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerToggleSprintEvent(PlayerToggleSprintEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).AddData("enabled", p.isSprinting()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerVelocityEvent(PlayerVelocityEvent p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).AddData("velocity", p.getVelocity()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerChatTabCompleteEvent(PlayerChatTabCompleteEvent p)
	{
		Packet pa = (this.GetPacket()).AddData("message", p.getChatMessage());
		Packet completions = this.GetPacket();
		try
		{
			Iterator<String> iter = p.getTabCompletions().iterator();
			int i = 0;
			while (iter.hasNext()) 
			{
				completions.AddData((new Integer(i)).toString(), iter.next());
				i++;
			}
		}
		catch (Exception e) { };
		pa.AddData("results", completions);
		this.AddEventToQueue(pa);
	}
	
	/*
	@EventHandler(priority = EventPriority.MONITOR)
	public void event(event p)
	{
		this.AddEventToQueue(this.NewPlayerEvenet(p).
							AddData("", ).
							AddData("", ));
	}
	*/
	
}