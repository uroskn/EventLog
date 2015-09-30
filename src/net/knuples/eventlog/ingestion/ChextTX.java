package net.knuples.eventlog.ingestion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.EnderChest;

import net.knuples.eventlog.EventLogPlugin;

public class ChextTX extends AbstractIngestionModule {

	private final Map<HumanEntity, ItemStack[]> containers = new HashMap<HumanEntity, ItemStack[]>();

	public ChextTX(EventLogPlugin plugin) {
		super(plugin);
		// TODO Auto-generated constructor stub
	}
	
	public static class ItemStackComparator implements Comparator<ItemStack>
	{
		public int compare(ItemStack a, ItemStack b) {
			final int aType = a.getTypeId(), bType = b.getTypeId();
			if (aType < bType) return -1;
			if (aType > bType) return 1;
			final short aData = rawData(a), bData = rawData(b);
			if (aData < bData) return -1;
			if (aData > bData) return 1;
			//if (!sameEnchants(a, b)) return -1;
			return 0;
		}
	}
	
	public static boolean sameEnchants(ItemStack i1, ItemStack i2)
	{
		return (i1.getEnchantments().equals(i2.getEnchantments()));
	}

	public static short rawData(ItemStack item) {
		return item.getType() != null ? item.getData() != null ? item.getDurability() : 0 : 0;
	}	

	public static ItemStack[] compressInventory(ItemStack[] items) {
		final ArrayList<ItemStack> compressed = new ArrayList<ItemStack>();
		for (final ItemStack item : items)
			if (item != null) {
				final int type = item.getTypeId();
				final short data = rawData(item);
				boolean found = false;
				for (final ItemStack item2 : compressed)
					if (type == item2.getTypeId() && data == rawData(item2) && (sameEnchants(item, item2))) {
						item2.setAmount(item2.getAmount() + item.getAmount());
						found = true;
						break;
					}
				if (!found)
					//compressed.add(new ItemStack(type, item.getAmount(), data));
					compressed.add(new ItemStack(item));
			}
		Collections.sort(compressed, new ItemStackComparator());
		return compressed.toArray(new ItemStack[compressed.size()]);
	}



	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onInventoryOpen(InventoryOpenEvent event) {
		if (event.getInventory() != null) {
			InventoryHolder holder = event.getInventory().getHolder();
			if (IsValidHolder(holder) || event.getInventory().getType() == InventoryType.ENDER_CHEST) {
				containers.put((Player) event.getPlayer(), compressInventory(event.getInventory().getContents()));
			}
		}
	}
	
	public boolean IsValidHolder(InventoryHolder holder)
	{
		return (holder instanceof BlockState || holder instanceof DoubleChest || holder instanceof EnderChest);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onInventoryClose(InventoryCloseEvent event) {
		InventoryHolder holder = event.getInventory().getHolder();
		if (IsValidHolder(holder) || event.getInventory().getType() == InventoryType.ENDER_CHEST) containers.remove(event.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void invEvent(InventoryClickEvent event) 
	{
		InventoryHolder holder = event.getInventory().getHolder();
		if (IsValidHolder(holder) || event.getInventory().getType() == InventoryType.ENDER_CHEST) ProcessTx(event);
	}
	
	public void ProcessTx(final InventoryEvent event)
	{
		Bukkit.getServer().getScheduler().runTask(this.plugin, new Runnable() {
        	public void run() { 
    			final Player player = (Player) event.getView().getPlayer();
    		    ItemStack[] before = containers.get(player);
    			if (before != null) {
    				ItemStack[]	after = compressInventory(event.getInventory().getContents());
    				ItemStack[]	diff = compareInventories(before, after);
    				for (final ItemStack item : diff) {
    					AddEventToQueue(CustomEventPacket("ContainerTx").
    							AddData("item",      SerializeItem(item)).
    							AddData("container", SerializeInventoryHolder(event.getView().getTopInventory().getHolder())).
    							AddData("player",    SerializeEntity(player)));
    				}
    				UpdateViewInv(event.getInventory(), after);
    			}
            }
		});
	}
	
	public void UpdateViewInv(Inventory inv, ItemStack[] is)
	{
		for (HumanEntity h : inv.getViewers())
		{
			if (!containers.containsKey(h)) continue;
			containers.remove(h);
			containers.put(h, is);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void InventoryMoveItemEvent(InventoryMoveItemEvent event)
	{
		if (!InvIngest.isSpaceAvailable(event.getDestination(), event.getItem())) return;
		if (event.getInitiator().getViewers().size() > 0)   UpdateViewInv(event.getInitiator(),   compressInventory(event.getInitiator().getContents()));
		if (event.getDestination().getViewers().size() > 0) UpdateViewInv(event.getDestination(), compressInventory(event.getDestination().getContents()));
		if (event.getSource().getViewers().size() > 0)      UpdateViewInv(event.getSource(),      compressInventory(event.getSource().getContents()));
	}

	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void invEvent(InventoryDragEvent event) 
	{
		InventoryHolder holder = event.getInventory().getHolder();
		if (IsValidHolder(holder) || event.getInventory().getType() == InventoryType.ENDER_CHEST) ProcessTx(event);
	}
		
	public static ItemStack[] compareInventories(ItemStack[] items1, ItemStack[] items2) {
		final ItemStackComparator comperator = new ItemStackComparator();
		final ArrayList<ItemStack> diff = new ArrayList<ItemStack>();
		final int l1 = items1.length, l2 = items2.length;
		int c1 = 0, c2 = 0;
		while (c1 < l1 || c2 < l2) {
			if (c1 >= l1) {
				diff.add(items2[c2]);
				c2++;
				continue;
			}
			if (c2 >= l2) {
				items1[c1].setAmount(items1[c1].getAmount() * -1);
				diff.add(items1[c1]);
				c1++;
				continue;
			}
			final int comp = comperator.compare(items1[c1], items2[c2]);
			if (comp < 0) {
				items1[c1].setAmount(items1[c1].getAmount() * -1);
				diff.add(items1[c1]);
				c1++;
			} else if (comp > 0) {
				diff.add(items2[c2]);
				c2++;
			} else {
				final int amount = items2[c2].getAmount() - items1[c1].getAmount();
				if (amount != 0) {
					items1[c1].setAmount(amount);
					diff.add(items1[c1]);
				}
				c1++;
				c2++;
			}
		}
		return diff.toArray(new ItemStack[diff.size()]);
	}

}
