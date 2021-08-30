package iotproject;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.server.resources.CoapExchange;

public class RegisterResource extends CoapResource {
	
	public RegisterResource(String name) {
		super(name);
		//this.setObservable(true);
 	}
	
 	public void handleGET(CoapExchange exchange) {
 		// accept request sending ACK
 		exchange.accept();
 		
 		InetAddress node_addr = exchange.getSourceAddress();
 		
 		// create a new request to get the node resources
 		CoapClient client = new CoapClient("coap://["+node_addr.getHostAddress()+"]:5683/.well-known/core");
 		CoapResponse response = client.get();

 		String code = response.getCode().toString();
 		if(!code.startsWith("2")) {
 			System.err.println("Registration Error: "+code);
 			return;
 		}
		DevResource newRes;
		boolean success=false;
		String[] resString=response.getResponseText().split(",");
 		for (String res:resString) {
 			if(res.contains("well-known"))
 				continue;
 			String path = res.split(";")[0].substring(1).split(">")[0].substring(1);
 			String info = res.split(">")[1].substring(1);
 			newRes = new DevResource(path,node_addr.getHostAddress(),info);
 			int node=newRes.getNode();
 			if(SmartShutter.devResources.containsKey(node)) {
	 			if(!SmartShutter.devResources.get(node).contains(newRes)) {
	 				SmartShutter.devResources.get(node).add(newRes);
	 				success=true; 				
	 			}
 			}else {
 				List<DevResource> l=new ArrayList<DevResource>();
 				l.add(newRes);
 				SmartShutter.devResources.put(node,l);
 				success=true; 	
 			}
 			if(success) {
	 			System.out.println("registered: "+ node_addr.getHostAddress()+" "+path);
				if(newRes.getInfo().contains("obs")) {
					SmartShutter.observableClients.add(0,new ObservingClient(newRes));
					SmartShutter.observableClients.get(0).startObserving();
				}
				if(newRes.getPath().contains("external")) {
					new UpdateThread(newRes).start();
				}
 			}
	 	}
 		
 	}
 	
	
}
