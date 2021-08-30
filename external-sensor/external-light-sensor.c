#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "coap-blocking-api.h"
#include "os/dev/button-hal.c"
#include "os/dev/leds.h" 
#include "sys/etimer.h"
/* Log configuration */
#include "coap-log.h"
#include "sys/log.h"
#define LOG_MODULE "External light sensor   "
#define LOG_LEVEL LOG_LEVEL_INFO

#define INTERVAL		(30 * CLOCK_SECOND)
/*
 * Resources to be activated need to be imported through the extern keyword.
 * The build system automatically compiles the resources in the corresponding sub-directory.
 */
extern coap_resource_t  res_external_light_sensor; 


#define SERVER ("coap://[fd00::1]:5683")
char* service_url = "/registration";
coap_message_t *response;

PROCESS(ext_sensor_process, "External-light-sensor");
AUTOSTART_PROCESSES(&ext_sensor_process);

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

PROCESS_THREAD(ext_sensor_process, ev, data)
{
	static coap_endpoint_t server;
	static coap_message_t request[1]; 
	static struct etimer timer;
	PROCESS_BEGIN();
	coap_activate_resource(&res_external_light_sensor,"sensors/light/external");
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
			
			if(ev== PROCESS_EVENT_TIMER){
				res_external_light_sensor.trigger();
				
			}
	}
	leds_off(LEDS_ALL);
  	PROCESS_END();
}
