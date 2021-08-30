package iotproject;

import java.util.List;
import java.util.Map;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

public class UpdateThread extends Thread {
	DevResource res;
	public UpdateThread(DevResource res) {this.res=res;}
	
	public void run(){
		while(true) {
			try {
				sleep(60*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			String string= res.getValues().get(res.getValues().lastKey()).split(": ")[1];
			for(Map.Entry<Integer,List<DevResource>> element:SmartShutter.devResources.entrySet()) {
		    	for(DevResource r : element.getValue()) {
			    	if(r.getPath().contains("internal")) {
			    		CoapClient client = new CoapClient(r.getCoapURI());
						CoapResponse update = client.post("out_lux="+string, MediaTypeRegistry.TEXT_PLAIN);
						String code = update.getCode().toString();
						if(!code.startsWith("2")) {
							System.err.println("Update Error: "+code);
						}
			    	}
		    	}
			}
		}
	}
		
}
