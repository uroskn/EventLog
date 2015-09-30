package net.knuples.eventlog;

import java.io.*;
import java.util.logging.Level;

public class SaveManager extends Thread {
	
	private EventLogPlugin plugin;
	private FileWriter     ostream;
	
	public  String         ofile;
	public  boolean        cleanup;
	public  boolean        closefile;
	
	public SaveManager(EventLogPlugin plugin)
	{
		this.plugin    = plugin;
		this.ofile     = "";
		this.cleanup   = false;
		this.closefile = false;
	}
	
	public void run()
	{
		ostream = null;
		String fn = ofile;
		try
		{
			ostream = new FileWriter(ofile, true);
		}
		catch (Exception e)
		{
			plugin.getLogger().log(Level.SEVERE, "Error while trying to open log file. Run /el write on to try again!");
			plugin.getLogger().log(Level.SEVERE, "Error message: " + e.getMessage());
			return;
		}
		while(true) 
		{
			if ((ostream != null) || (!this.closefile))
			{
				Packet pkt = null;
				try
				{
				  if (!this.cleanup) pkt = plugin.queue.take();
				  else pkt = plugin.queue.remove();
				  try
				  {
					  ostream.write(pkt.toString() + "\n");
					  ostream.flush();
					  plugin.outgoing.Count();
				  }
				  catch (IOException e)
				  {
				      plugin.getLogger().log(Level.SEVERE, "Error while writing to log file! Aborting to prevent console spam!");
				      plugin.getLogger().log(Level.SEVERE, "To restart console log writing use /el write on!");
					  plugin.getLogger().log(Level.SEVERE, "Error message: " + e.getMessage());
					  break;
				  }
				}
				catch (Exception e)
				{
					if (!(e instanceof InterruptedException)) e.printStackTrace();
					break;
				}
			}
			else break;
		}
		plugin.getLogger().log(Level.INFO, "Closing outut file: " + fn);
		try
		{
			ostream.flush();
			ostream.close();
		}
		catch (IOException e) { 
			plugin.getLogger().log(Level.SEVERE, "Error while closing log file!");
			plugin.getLogger().log(Level.SEVERE, "Error message: " + e.getMessage());
		}
		ostream = null;
	    plugin.getLogger().log(Level.INFO, "Succesfully closed");
	}
}
