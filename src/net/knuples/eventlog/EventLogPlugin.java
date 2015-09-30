package net.knuples.eventlog;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import net.knuples.eventlog.ingestion.*;
import net.knuples.eventlog.SaveManager;
import net.knuples.misc.StatHashMap.MapStat;

import java.util.ArrayList;
import java.util.concurrent.*;

public class EventLogPlugin extends JavaPlugin {
		
	public StatusCounter        		  incoming;
	public StatusCounter        		  outgoing;
	public LinkedBlockingQueue<Packet>    queue;
	
	private int         	  dropped_events;
	private long        	  ptotal;
	private SaveManager       mngr;
	
	private ArrayList<AbstractIngestionModule> abmod;
	
	private ComplexIngestion comping;
	
    @Override
    public void onEnable() {
    	this.saveDefaultConfig();
    	
        incoming = new StatusCounter();
        outgoing = new StatusCounter();
        
        queue    = new LinkedBlockingQueue<Packet>(this.getConfig().getInt("queue-size"));  
        abmod    = new ArrayList<AbstractIngestionModule>();
                
        /* Neat trick, eh? :-) */
        abmod.add(new BlockEventsIngestion(this));
        abmod.add(new PlayerMoveIngest(this));
        abmod.add(new EnchantmentIngestion(this));
        abmod.add(new EntityIngest(this));
        abmod.add(new HangingIngest(this));
        abmod.add(new WorldIngest(this));
        abmod.add(new PlayerIngest(this));
        abmod.add(new ChextTX(this));
        abmod.add(new InvIngest(this));
        
        comping = new ComplexIngestion(this);
        abmod.add(comping);
    	    	
    	dropped_events = 0;
    	
    	mngr       = new SaveManager(this);
    	mngr.ofile = this.getConfig().getString("output-file");
    	if (this.getConfig().getBoolean("auto-write")) EnableWriting();
 	       	    	
    	getLogger().info("ELP done, monitoring events.");
    }
 
    @Override
    public void onDisable() 
    {
    	for (AbstractIngestionModule mod : abmod) mod.StopCollection();
    	if (mngr.isAlive())
    	{
    		getLogger().info("Waiting for outputtig to finish...");
    		mngr.cleanup = true;
    		mngr.interrupt();
    		try
    		{
    			mngr.join();
    		}
    		catch (Exception e) { }
    	}
    	else if (!queue.isEmpty()) getLogger().warning("Event log is not empty! " + queue.size() + " events dropped"); 
    	getLogger().info("Done");
    }
    
    private void DisableWriting()
    {
    	if (mngr.isAlive())
    	{
    		mngr.closefile = true;
    		mngr.interrupt();
    		try
    		{
    			mngr.join();
    		}
    		catch (Exception e) { };
    	}
    }
    
    private void EnableWriting()
    {
    	if (!mngr.isAlive()) 
    	{
    		String str = mngr.ofile;
    		mngr = null;
    		mngr = new SaveManager(this);
    		mngr.ofile = str;
    		mngr.start();
    	}
    }
    
    private boolean PrintHelp(CommandSender sender)
    {
    	sender.sendMessage("Usage : /el <command>");
    	sender.sendMessage(" /el queue           : Print event proessing rate");
    	sender.sendMessage(" /el filename [file] : Change or print outputing filename");
    	sender.sendMessage(" /el write [on|off]  : Toggle writing (DANGEROUS!)");
    	sender.sendMessage(" /el resetcounter    : Resets total counter");
    	return true;
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) 
    {
    	if ((cmd.getName().equalsIgnoreCase("eventlog")) && (args.length != 0))
    	{
    		if (args[0].equalsIgnoreCase("queue")) 
    		{
    			sender.sendMessage("--------------------------------------------");
    			sender.sendMessage(queue.size() + " waiting, " + dropped_events + " drops, total " + ptotal + " events captured.");
    			sender.sendMessage("Incoming rate @ " + incoming.GetRate() + " e/s, Outgoing rate @ " + outgoing.GetRate() + " e/s");
    			sender.sendMessage("--------------------------------------------");
    			sender.sendMessage("Entity tracking tracks " + comping.entitylist.size() + " entities");
    			sender.sendMessage("Total misstracks: " + comping.missdissapear + ", last run: " + comping.lastrun);
    			//if ((args.length > 1) && (args[1].equalsIgnoreCase("debug")))
    			{
    				MapStat s = comping.entitylist.mapStatistic();
    				String str = "(R)";
    				if (comping.cooldown > 0) str = "";
    				sender.sendMessage("Map size: " + comping.entitylist.mapSize() + ", " + s.collisions + " collisons with " + s.collidingentries +" entries " + str);
    			}
    			sender.sendMessage("--------------------------------------------");
    		}
    		else if (args[0].equalsIgnoreCase("resetcounter"))
    		{
    			if ((sender instanceof Player) && (!sender.isOp())) return false;
    			sender.sendMessage("Counter restarted");
    			ptotal = 0;
    		}
    		else if (args[0].equalsIgnoreCase("write"))
    		{
    			if ((sender instanceof Player) && (!sender.isOp())) return false;
    			if (args.length < 2) 
    			{
    				if (mngr.isAlive()) sender.sendMessage("Outputting thread is running");
    				else sender.sendMessage("Outputting is halted.");
    			}
    			else
    			{
    				if ((mngr.isAlive()) && (args[1].equalsIgnoreCase("off")))
    				{
    					sender.sendMessage("Stopping writing...");
    					this.DisableWriting();
    				}
    				if (args[1].equalsIgnoreCase("on")) 
    				{
    					sender.sendMessage("Starting with writing...");
    					this.EnableWriting();
    				}
    			}
    		}
    		else if (args[0].equalsIgnoreCase("filename"))
    		{
    			if ((sender instanceof Player) && (!sender.isOp())) return false;
    			if (args.length == 1) sender.sendMessage("Outputting file is: " + mngr.ofile);
    			else
    			{
    				boolean ew = mngr.isAlive();
					sender.sendMessage("Stopping writing and changing filename...");
					this.DisableWriting();
					mngr.ofile = args[1];
					getConfig().set("output-file", args[1]);
					this.saveConfig();
					if (ew) this.EnableWriting();
    			}
    		}
    		else return PrintHelp(sender);
    		return true;
    	}
        return false;
    }
	
	public void EnqueueEvent(Packet packet)
	{
		incoming.Count();
		ptotal++;
		if (!queue.offer(packet)) dropped_events++;
	}
	
}
