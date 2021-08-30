package iotproject;

import org.eclipse.californium.core.CaliforniumLogger;
import org.eclipse.californium.core.CoapServer;


public class Server extends CoapServer {
	static {
		CaliforniumLogger.disableLogging();
	}
	
	public Server(int port) {
		super(port);
		this.add(new RegisterResource("registration"));
	}
	
}
