package iotproject;

import java.sql.Timestamp;
import java.util.TreeMap;

public class DevResource {


	private String path;
	private String address;
	private String info;
	private TreeMap<String,String> values = new TreeMap<String,String>();
	
	public DevResource (String path, String addr, String info){
		this.path = path;  //  /gate
		this.address = addr;
		this.info = info;
	}
	
	synchronized public void setValues(TreeMap<String,String> v) {
  		long last5mins = System.currentTimeMillis() - (60000 * 5);
		if( Timestamp.valueOf(v.firstEntry().getKey()).getTime() < last5mins)
				   v.remove(v.firstEntry().getKey());
		this.values = v; 
	}
	
	public String getInfo(){ return this.info; }
	
	public String getPath(){ return this.path; }
	
	public int getNode() {
		String[] addr = this.address.split(":");
		return Integer.parseInt(addr[addr.length-1],16);
	}
	
	public String getAddress(){ return this.address; }

	public String getCoapURI(){ return "coap://[" + this.address+"]:5683/"+ this.path;}
	
	synchronized public TreeMap<String,String> getValues(){ return this.values; }
	
	@Override
	public boolean equals(Object o){
		DevResource d = (DevResource)o;
		return (this.path.equals(d.path) && this.address.equals(d.address));
	}

	@Override
	public String toString() {
		String[] addr = this.address.split(":");
		return "Node:"+ Integer.parseInt(addr[addr.length-1],16) +" Path:"+ this.path+", "+this.info;
	}
	
	public String shortDetails() {
		String[] addr = this.address.split(":");
		return "Node: "+ Integer.parseInt(addr[addr.length-1],16) +" Path:"+ this.path;
	}
	
}