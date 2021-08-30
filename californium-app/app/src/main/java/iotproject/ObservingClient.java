package iotproject;


import java.sql.Timestamp;
import java.util.TreeMap;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

public class ObservingClient extends CoapClient {
	private DevResource resource;
	CoapObserveRelation relation;
	
	public ObservingClient(DevResource res) {
		super(res.getCoapURI());
		this.resource = res;
	}
	
	public void startObserving() {
		relation = this.observe(new CoapHandler(){
			
			public void onLoad(CoapResponse response) {
					try {
						String code = response.getCode().toString();
						if(code.startsWith("2")) {
							if(response.getOptions().getAccept() == MediaTypeRegistry.APPLICATION_JSON|| response.getOptions().getAccept() == MediaTypeRegistry.UNDEFINED)
								addResourceFromJson(response.getResponseText());
							else if (response.getOptions().getAccept() == MediaTypeRegistry.TEXT_PLAIN)
								addResourceFromTextMsg(response.getResponseText());
							else System.err.println("ObsHandler: Accept format not supported");
						}
						else {
							System.err.println("Observing: Response Error "+code);
						}
						
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}       	
			}
			
			public void onError() {
				// TODO Auto-generated method stub
				System.err.println("Observe Error");
			}
			
			private void addResourceFromJson(String response) throws ParseException {
				String value = null;
				Timestamp timestamp;
				JSONObject jo = (JSONObject) JSONValue.parseWithException(response);
				if( jo.containsKey("timestamp") )
		        	timestamp = new Timestamp(((Long) jo.get("timestamp"))*1000); 
		        else {
		        	System.err.println("Error Observing Client: No timestamp");
		        	return;
		        }
				
				if(resource.getPath().contains("sensors")) {
					if(resource.getPath().contains("light"))	{
						if( jo.containsKey("lux") )
							value ="lux: " +jo.get("lux").toString(); 
						else {
							System.err.println("Error Observing Client - Light Sensor: No illumination value (lux)");
							return;
						}
					}else if(resource.getPath().contains("presence")){
						if( jo.containsKey("obstacle") )
							value ="obstacle: " +((jo.get("obstacle").toString().equals("1")) ? "PRESENT" : "ABSENT"); 
						else {
							System.err.println("Error Observing Client - Presence Sensor: No obstacle information");
							return;
						}	
					}
				}else if(resource.getPath().contains("actuator")) {
					if( jo.containsKey("position") )
						value = "position: "+jo.get("position").toString();
					
					else {
						System.err.println("Error Observing Client -Actuator: No position");
						return;
					}
					if( jo.containsKey("mode") )
						value =value.concat(" mode: "+((jo.get("mode").toString().equals("1")) ? "AUTO" : "MANUAL"));
					
					else {
						System.err.println("Error Observing Client -Actuator: No mode");
						return;
					}
					if( jo.containsKey("target_lux") )
						value =value.concat(" target_lux: "+jo.get("target_lux").toString());
					
					else {
						System.err.println("Error Observing Client -Actuator: No target_lux");
						return;
					}
					
				}
				TreeMap<String,String> resourceValues = resource.getValues();
		  		resourceValues.put(timestamp.toString(), value);
		  		SmartShutter.devResources.get(resource.getNode()).get(SmartShutter.devResources.get(resource.getNode()).indexOf(resource)).setValues(resourceValues);

			}
			
			private void addResourceFromTextMsg(String response) {
				String value = null; 
		        Timestamp timestamp;
		        
		        String[] maps = response.split(",");
		        if(resource.getPath().contains("sensors")) {
					if(resource.getPath().contains("light")) {
						if(maps[0].contentEquals("lux"))
			        		value = "lux: "+maps[0].split("=")[1];
			        	else {
							System.err.println("Error Observing Client - Light Sensor: No illumination value (lux)");
							return;
						}
					}else if(resource.getPath().contains("presence")){
						if(maps[0].contentEquals("obstacle"))
			        		value = "obstacle: "+((maps[0].split("=")[1].equals("1"))? "PRESENT":"ABSENT");
			        	else {
							System.err.println("Error Observing Client - Presence Sensor: No obstacle information");
							return;
						}	
					}
		        	if( maps[1].contentEquals("timestamp") )
		            	timestamp = new Timestamp((Long.parseLong(maps[1].split("=")[1]))*1000); 
		            else {
		            	System.err.println("Error Observing Client - Sensor: No timestamp");
		            	return;
		            }
		        }else if(resource.getPath().contains("actuator")) {
		        	if(maps[0].contentEquals("position"))
		        		value = "position: "+maps[0].split("=")[1];
		        	else {
						System.err.println("Error Observing Client -Actuator: No position");
						return;
		        	}
		        	if(maps[1].contentEquals("mode"))
		        		value =value.concat(" mode: "+ ((maps[1].split("=")[1].equals("1"))? "AUTO":"MANUAL"));
		        	else {
						System.err.println("Error Observing Client -Actuator: No mode");
						return;
		        	}
		        	if(maps[2].contentEquals("target_lux"))
		        		value = value.concat(" target_lux: "+maps[2].split("=")[1]);
		        	else {
						System.err.println("Error Observing Client -Actuator: No target_lux");
						return;
					}
		        	if(maps[3].contentEquals("timestamp") )
		             	timestamp = new Timestamp((Long.parseLong(maps[3].split("=")[1]))*1000); 
		            else {
		             	System.err.println("Error Observing Client -Actuator: No timestamp");
		             	return;
		            }
		        }else {
					System.err.println("Can't find node type");
					return;
		        }
		        TreeMap<String,String> resourceValues = resource.getValues();
		  		resourceValues.put(timestamp.toString(), value);
		  		SmartShutter.devResources.get(resource.getNode()).get(SmartShutter.devResources.get(resource.getNode()).indexOf(resource)).setValues(resourceValues);
		}
		});
		System.out.println("Observing "+resource.toString());
	}
	

	public void stopObserving() {
		this.relation.proactiveCancel();
		System.out.println("[Stopped observing "+resource.toString());
	}
	
	@Override
	public boolean equals(Object o) {
		ObservingClient observable = (ObservingClient)o;
		return (observable.resource.equals(this.resource));
	}
	
}
