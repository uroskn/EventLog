package net.knuples.eventlog;

import java.util.*;

public class Packet {
  
  private ArrayList<String> keys;
  private ArrayList<Object> data;
	
  public Packet() { 
	  this.keys = new ArrayList<String>(15);
	  this.data = new ArrayList<Object>(15);
  }
  
  /* For profiler */
  private void AddKey(String key) { this.keys.add(key); }
  private void AddData(Object value) { this.data.add(value); } 
  
  public Packet AddData(String key, Object value) 
  { 
	  this.AddKey(key);
	  this.AddData(value);
	  return this; 
  }
  
  public String toString()
  {
	  String str = "{";
	  for (int i = 0; i < keys.size(); i++)
	  {
		  str = str + keys.get(i) + ":";
		  try
		  {
			  if (data.get(i) instanceof Packet) str = str + data.get(i).toString() + ",";
			  else str = str + "\"" + data.get(i).toString().replace("\"", "\\\"") + "\",";
		  }
		  catch (NullPointerException e)
		  {
			  str = str + "null,";
		  }
	  }
	  return str + "}";
  }
}
