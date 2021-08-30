package iotproject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

public class SmartShutter {
	
	public static final String[] commands = {"list","status","move","AUTO","MANUAL","setTarget","PRESENT","ABSENT","exit"};
	public static Map<Integer,List<DevResource>> devResources = new TreeMap<Integer,List<DevResource>>();
	public static List<ObservingClient> observableClients = new ArrayList<ObservingClient>();
	static Server server= new Server(5683); 
	
	public static void printCommands() {
        System.out.println("----------------------------------------------------------------------------------------------------------------");
		System.out.println("AVAILABLE COMMANDS: (use [node] -1 to execute for all compatible devices)");
		System.out.println(commands[0]+" - Show Available Resources");
		System.out.println(commands[1]+" [node] - print the status of resources at [node]");
		System.out.println(commands[2]+" [node] [UP|DOWN] [value](1:10) - Move [UP|DOWN] shutter at [node] by [value]");
		System.out.println(commands[3]+" [node] - Enable auto mode for shutter [node]");
		System.out.println(commands[4]+" [node] - Disable auto mode for shutter index [node]");
		System.out.println(commands[5]+" [node] [value](100:1000) - set target value for actuator [node] at [value]");
		System.out.println(commands[6]+" [node]  - Add obstacle on [node]");
		System.out.println(commands[7]+" [node]  - Remove obstacle from [node]");
		System.out.println(commands[8]+" - exit");
		System.out.println("----------------------------------------------------------------------------------------------------------------");
	}
	
	public static void showResourceStatus(DevResource r) {
		System.out.println(r.shortDetails());
		System.out.println("TIMESTAMP\t\t\tINFO");
		Map<String,String> v = r.getValues();
		for(String key : v.keySet()) {
			System.out.println(key + "\t\t " + v.get(key));
		}
		System.out.println();
	}
	
	public static void switchMode(String mode, DevResource res) {
		CoapClient client = new CoapClient(res.getCoapURI());
		CoapResponse response = client.post("mode="+mode, MediaTypeRegistry.TEXT_PLAIN);
		String code = response.getCode().toString();
		if(code.startsWith("2")) {
			System.out.println("The shutter is now in "+mode+" mode");
		}else {
			System.err.println("Error: "+code);
		}
	}
	
	public static void switchObstacle(String mode, DevResource res) {
		CoapClient client = new CoapClient(res.getCoapURI());
		CoapResponse response = client.post("obstacle="+mode, MediaTypeRegistry.TEXT_PLAIN);
		String code = response.getCode().toString();
		if(code.startsWith("2")) {
			System.out.println("The obstacle is now "+mode);
		}else {
			System.err.println("Error: "+code);
		}
	}
	
	public static void setTarget(int value, DevResource res) {
		CoapClient client = new CoapClient(res.getCoapURI());
		CoapResponse response = client.post("target_lux="+value, MediaTypeRegistry.TEXT_PLAIN);
		String code = response.getCode().toString();
		if(code.startsWith("2")) {
			System.out.println("The shutter target lux value is now "+value);
		}else {
			System.err.println("Error: "+code);
		}
	}
	
	public static void moveShutter(String mode, int amount, DevResource res) {
		CoapClient client = new CoapClient(res.getCoapURI());
		CoapResponse response = client.post("action="+mode+"&amount="+amount, MediaTypeRegistry.TEXT_PLAIN);
		String code = response.getCode().toString();
		if(!code.startsWith("2")) {
			System.err.println("Error: "+code);
		}
		else {
			System.out.println("Shutter Moved");
		}
	}
	
	public static void showAvailableResources() {
		for(Map.Entry<Integer,List<DevResource>> element:devResources.entrySet()) {
    		for(DevResource r : element.getValue())
				System.out.println(r.toString());
    		System.out.println();
		}
	}
	
	private static void startServer() {
		new Thread() {
			public void run() {
				
				server.start();
			}
		}.start();
	}
	public static void main(String[] args) throws IOException, InterruptedException {
		
		startServer();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		printCommands();
		while(true) {
			try {
				String readCommand = br.readLine();
				String[] splittedCommand = readCommand.split(" ");
				if(devResources.size() == 0) { 
					System.out.println("NO RESOURCE REGISTERED");
				}else if(splittedCommand[0].equals(commands[0])) {
		        	showAvailableResources();
				}else if(splittedCommand[0].equals(commands[2])){
					if(splittedCommand.length>3&&(splittedCommand[2].equals("UP")||splittedCommand[2].equals("DOWN"))) {
						int index=Integer.parseInt(splittedCommand[1]);
						int amount=Integer.parseInt(splittedCommand[3]);
						if(amount>10||amount<1){
							System.err.println("Amount must be a number between 1 and 10.");
							printCommands();
						}else {
							if(SmartShutter.devResources.containsKey(index)) {
					        	for(DevResource r : devResources.get(index))
				        			if(r.getPath().contains("actuator"))
				        				 moveShutter(splittedCommand[2],amount, r);
				        	}else if (index == -1) {
				        		for(Map.Entry<Integer,List<DevResource>> element:devResources.entrySet())
					        		for(DevResource r : element.getValue())
					        			if(r.getPath().contains("actuator")) 
					        				 moveShutter(splittedCommand[2],amount, r);
				        	}
						}
					}else {
						System.err.println("Wrong command format, try again.");
						printCommands();
					}				
				}else if(splittedCommand[0].equals(commands[5])){
					if(splittedCommand.length>2) {
						int index=Integer.parseInt(splittedCommand[1]);
						int amount=Integer.parseInt(splittedCommand[2]);
						if(amount>1000||amount<100)
						{
							System.err.println("Incorrect value: must be a number between 100 and 1000.");
							printCommands();
						}else {
							if(SmartShutter.devResources.containsKey(index)) {
					        	for(DevResource r : devResources.get(index))
				        			if(r.getPath().contains("actuator"))
							        	setTarget(amount, r);
				        			
				        	}else if (index == -1) {
				        		for(Map.Entry<Integer,List<DevResource>> element:devResources.entrySet())
					        		for(DevResource r : element.getValue())
					        			if(r.getPath().contains("actuator")) 
					        				setTarget(amount, r);
				        	}
				  
				        	
						}
					}else {
						System.err.println("Wrong command format, try again.");
						printCommands();
					}
					
				}else if(splittedCommand[0].equals(commands[1])){
						if(splittedCommand.length>1) {
							int index=Integer.parseInt(splittedCommand[1]);
							if(SmartShutter.devResources.containsKey(index)) {
					        	for(DevResource r : devResources.get(index))
					        		showResourceStatus( r);
				        	}else if (index == -1){
				        		for(Map.Entry<Integer,List<DevResource>> element:devResources.entrySet()) {
					        		for(DevResource r : element.getValue()) {
				        				showResourceStatus( r);
					        		}
					        		System.out.println();
				        		}
				        	}
						}
				        else {
							System.err.println("Wrong command format, try again.");
							printCommands();
						}			
				}else if(splittedCommand[0].equals(commands[3])||splittedCommand[0].equals(commands[4])){
					if(splittedCommand.length>1) {
						int index=Integer.parseInt(splittedCommand[1]);
						if(SmartShutter.devResources.containsKey(index)) {
							for(DevResource r : devResources.get(index))
			        			if(r.getPath().contains("actuator"))
			        					switchMode(splittedCommand[0], r);
			        	}else if (index == -1){
			        		for(Map.Entry<Integer,List<DevResource>> element:devResources.entrySet()) 
				        		for(DevResource r : element.getValue()) 
				        			if(r.getPath().contains("actuator"))
				        				switchMode(splittedCommand[0], r);
			        	}
					}else {
						System.err.println("Wrong command format, try again.");
						printCommands();
					}
				}else if(splittedCommand[0].equals(commands[6])||splittedCommand[0].equals(commands[7])){
					if(splittedCommand.length>1) {
						int index=Integer.parseInt(splittedCommand[1]);
						if(SmartShutter.devResources.containsKey(index)) {
							for(DevResource r : devResources.get(index))
			        			if(r.getPath().contains("presence"))
			        					switchObstacle(splittedCommand[0], r);
			        	}else if (index == -1){
			        		for(Map.Entry<Integer,List<DevResource>> element:devResources.entrySet()) 
				        		for(DevResource r : element.getValue()) 
				        			if(r.getPath().contains("presence"))
				        				switchObstacle(splittedCommand[0], r);
			        	}
					}else {
						System.err.println("Wrong command format, try again.");
						printCommands();
					}
				}else if(splittedCommand[0].equals(commands[8])) {
					System.out.println("Stopping Program...");
		        	System.exit(0);
				}
				else {
					System.err.println("Wrong command, try again.");
					printCommands();
				}
				
			}catch(Exception e) {
				System.err.println("Something went wrong.");
				e.printStackTrace();
			}
			
		}
	}
}