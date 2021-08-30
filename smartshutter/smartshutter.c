#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "coap-blocking-api.h"
#include "os/dev/button-hal.c"
#include "os/dev/leds.h" 
#include "sys/etimer.h"
#include "./devices/internal-sensor.h"
#include "./devices/shutter-actuator.h"
#include "./devices/presence-sensor.h"
/* Log configuration */
#include "coap-log.h"
#include "sys/log.h"
#define LOG_MODULE "SmartShutter   "
#define LOG_LEVEL LOG_LEVEL_INFO

#define INTERVAL		(30 * CLOCK_SECOND)
/*
 * Resources to be activated need to be imported through the extern keyword.
 * The build system automatically compiles the resources in the corresponding sub-directory.
 */
extern coap_resource_t  res_internal_light_sensor;
extern coap_resource_t  res_shutter_control;
extern process_event_t  AUTOMATIC_SHUTTER_EVENT;
extern coap_resource_t  res_presence_sensor;
extern process_event_t OBSTACLE_EVENT;
#define SERVER ("coap://[fd00::1]:5683")
char* service_url = "/registration";
coap_message_t *response;

PROCESS(device_process, "SmartShutter");
AUTOSTART_PROCESSES(&device_process);

// Define a handler to handle the response from the server
void client_chunk_handler(coap_message_t *r)
{
  if(r == NULL) {
    LOG_INFO("Request timed out\n");
    return;
  }
	
	LOG_INFO("REGISTERED\n");
	response = r;
}

PROCESS_THREAD(device_process, ev, data)
{
	static coap_endpoint_t server;
	static coap_message_t request[1]; 
	static struct etimer timer;
	button_hal_button_t* btn;
	PROCESS_BEGIN();


	btn = button_hal_get_by_index(0);
	if(btn){
		
		LOG_INFO("%s on pin %u with ID=0, Logic=%s, Pull=%s\n",
			BUTTON_HAL_GET_DESCRIPTION(btn), btn->pin,
			btn->negative_logic ? "Negative" : "Positive",
			btn->pull == GPIO_HAL_PIN_CFG_PULL_UP ? "Pull Up" : "Pull Down");
	
	}
	coap_activate_resource(&res_shutter_control,"actuator/shutter"); 
	coap_activate_resource(&res_internal_light_sensor,"sensors/light/internal");
	coap_activate_resource(&res_presence_sensor,"sensors/presence");
  


	coap_endpoint_parse(SERVER, strlen(SERVER), &server);
	
	coap_init_message(request, COAP_TYPE_CON, COAP_GET, 0);
	coap_set_header_uri_path(request, service_url);
	do{
		COAP_BLOCKING_REQUEST(&server, request, client_chunk_handler);
	}while(response == NULL);
	
	leds_single_on(LEDS_GREEN);
	while(1){

			etimer_set(&timer, INTERVAL);
			PROCESS_WAIT_EVENT();
			
			if(get_mode() && ev == PROCESS_EVENT_TIMER){
				LOG_INFO("AUTO:Moving shutters (timer expired)\n");
				while(!isOpen()&&(int_collect_data()<(get_target()-50))){
					shutter_up();
				}
				while(!isClosed()&&!get_obstacle()&&(int_collect_data()>(get_target()+50))){
					shutter_down();
				}
			} 
			if(ev== PROCESS_EVENT_TIMER){
				res_shutter_control.trigger();
				res_internal_light_sensor.trigger();
				res_presence_sensor.trigger();
				
			}
			else if(ev == button_hal_release_event) {
				btn = (button_hal_button_t *)data;
				if( get_mode() ){
					set_manual();
					
					LOG_INFO("Release event (SmartShutter mode: MANUAL)\n");
				}else{
					set_automatic();
					
					LOG_INFO("Release event (SmartShutter mode: AUTO)\n");
				}
			} 
			else if(ev == AUTOMATIC_SHUTTER_EVENT){
				LOG_INFO("Automatic Mode On/Off event\n");
				res_shutter_control.trigger();
			}
			else if(ev == OBSTACLE_EVENT){
				LOG_INFO("Obstacle present/absent event\n");
				res_presence_sensor.trigger();
			}
	}
	leds_off(LEDS_ALL);
  	PROCESS_END();
}
