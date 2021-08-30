#include "contiki.h"
#include <stdlib.h>
#include <string.h>
#include "coap-engine.h"
#include "time.h"
#include "sys/log.h"
#define LOG_MODULE "Presence Sensor"
#define LOG_LEVEL LOG_LEVEL_DBG
#include "../devices/presence-sensor.h"


static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_post_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_event_handler(void);


EVENT_RESOURCE(res_presence_sensor,
         "title=\"Presence Sensor: ?POST/PUT obstacle=PRESENT|ABSENT\";rt=\"Obstacle Detection\";obs",
         res_get_handler,
         res_post_put_handler,
         res_post_put_handler,
         NULL,
         res_event_handler);
static int32_t obs_counter = 0;
static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	if(request != NULL){  
		LOG_DBG("GET Request Received\n");
	}
	unsigned int accept = -1;
	coap_get_header_accept(request, &accept);
	unsigned long timestamp = (unsigned long)time(NULL);
	if(accept == TEXT_PLAIN){
		coap_set_header_content_format(response, TEXT_PLAIN);
		snprintf((char *)buffer, COAP_MAX_CHUNK_SIZE, "obstacle=%d, timestamp=%lu", 
			 get_obstacle(), timestamp);

		coap_set_payload(response, (uint8_t *)buffer, strlen((char *)buffer));
		LOG_INFO("presence:sending text\n");
	}
	 else if(accept == APPLICATION_XML){
		coap_set_header_content_format(response, APPLICATION_XML);
		snprintf((char *)buffer, COAP_MAX_CHUNK_SIZE, "<obstacle=\"%d\"/><timestamp=\"%lu\"/>", 
			get_obstacle(), timestamp);
		coap_set_payload(response, buffer, strlen((char *)buffer));
		LOG_INFO("presence:sending xml\n");
	} 
	else if(accept == -1 || accept == APPLICATION_JSON){
		coap_set_header_content_format(response, APPLICATION_JSON);
		snprintf((char *)buffer, COAP_MAX_CHUNK_SIZE, "{\"obstacle\":%d,\"timestamp\":%lu}", 
			get_obstacle(), timestamp);

		coap_set_payload(response, buffer, strlen((char *)buffer));
	} 
	else{
		coap_set_status_code(response, NOT_ACCEPTABLE_4_06);
		const char *msg = "Unsupported Content Type";
		coap_set_payload(response, msg, strlen(msg));
	}
}


static void res_post_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	if(request != NULL){
	LOG_DBG("POST/PUT Request Recieved\n");
	}
	size_t len = 0;
	const char *command = NULL;
	int success = 1;


	if((len = coap_get_post_variable(request, "obstacle", &command))) {
		LOG_DBG("obstacle %s\n", command);

		if(strncmp(command, "PRESENT", len) == 0) {
			set_obstacle(1);
		} else if(strncmp(command, "ABSENT", len) == 0) {
			set_obstacle(0);
		} else {
			success = 0;
		}
	}else {
		success = 0;
  	} 
  
	if(!success){
	coap_set_status_code(response, BAD_REQUEST_4_00);
	}else{
  	coap_set_status_code(response, CHANGED_2_04);
  	}
}
static void res_event_handler(void)
{
	obs_counter++;
	coap_notify_observers(&res_presence_sensor);
}
