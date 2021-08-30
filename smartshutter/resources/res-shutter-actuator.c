#include "contiki.h"
#include <stdlib.h>
#include <string.h>
#include "coap-engine.h"
#include "time.h"
#include "sys/log.h"
#define LOG_MODULE "Shutter Controller"
#define LOG_LEVEL LOG_LEVEL_DBG
#include "../devices/shutter-actuator.h"


static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_post_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_event_handler(void);


EVENT_RESOURCE(res_shutter_control,
         "title=\"Shutter Controller: ?POST/PUT action=UP|DOWN&amount=[1:10](default=1) mode=AUTO|MANUAL target_lux=[100:1000]\";rt=\"Shutter Control\";obs",
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
		snprintf((char *)buffer, COAP_MAX_CHUNK_SIZE, "position=%d, mode=%d, target_lux=%d, timestamp=%lu", 
			 get_position(), get_mode(), get_target(), timestamp);

		coap_set_payload(response, (uint8_t *)buffer, strlen((char *)buffer));
		LOG_INFO("actuator:sending text\n");
	}
	 else if(accept == APPLICATION_XML){
		coap_set_header_content_format(response, APPLICATION_XML);
		snprintf((char *)buffer, COAP_MAX_CHUNK_SIZE, "<position=\"%d\"/><mode=\"%d\"/><target_lux=\"%d\"/><timestamp=\"%lu\"/>", 
			get_position(), get_mode(), get_target(), timestamp);
		coap_set_payload(response, buffer, strlen((char *)buffer));
		LOG_INFO("actuator:sending xml\n");
	} 
	else if(accept == -1 || accept == APPLICATION_JSON){
		coap_set_header_content_format(response, APPLICATION_JSON);
		snprintf((char *)buffer, COAP_MAX_CHUNK_SIZE, "{\"position\":%d,\"mode\":%d,\"target_lux\":%d,\"timestamp\":%lu}", 
			get_position(), get_mode(), get_target(), timestamp);

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

	if((len = coap_get_post_variable(request, "action", &command)))	{ 
		LOG_DBG("action %s\n", command);

		if(strncmp(command, "UP", len) == 0) {
			if((len = coap_get_post_variable(request, "amount", &command)))	{
				int amount=atoi(command);
				if(1<=amount&&amount<=10){			
					for (int i=0;i<amount;i++) 
					{shutter_up();}
				} else {
				success = 0;
				}
			} else {
			shutter_up();
			}
		} else if(strncmp(command, "DOWN", len) == 0) { 
			if((len = coap_get_post_variable(request, "amount", &command)))	{
				int amount=atoi(command);
				if(1<=amount&&amount<=10){			
					for (int i=0;i<amount;i++) 
					{shutter_down();}
				} else {
				success = 0;
				}
			} else {
			shutter_down();
			}
		} else {
		success = 0;
		}
	} else if((len = coap_get_post_variable(request, "mode", &command))) {
		LOG_DBG("mode %s\n", command);

		if(strncmp(command, "AUTO", len) == 0) {
		set_automatic();
		} else if(strncmp(command, "MANUAL", len) == 0) {
		set_manual();
		} else {
		success = 0;
		}
	} else if((len = coap_get_post_variable(request, "target_lux", &command))) {
		LOG_DBG("target_lux %s\n", command);

		int amount=atoi(command);
		if(100<=amount&&amount<=1000){			
		set_target(amount);
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
	LOG_DBG("shutter_actuator: res event handler called\n");
	coap_notify_observers(&res_shutter_control);
}
