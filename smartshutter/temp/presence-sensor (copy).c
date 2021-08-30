#include "presence-sensor.h"
#include "contiki.h"
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include "os/dev/leds.h" 
#define OPEN		10
#define CLOSED		0
process_event_t OBSTACLE_EVENT;
extern struct process device_process;
static bool present = 0;

void init_presence()
{
	OBSTACLE_EVENT = process_alloc_event();
}

bool get_obstacle()
{
	return present;
}

void set_obstacle(bool obst)
{
	present=obst;	
	if(obst)
		leds_single_on(LEDS_RED);
	else
		leds_single_off(LEDS_RED);
	process_post(&device_process,OBSTACLE_EVENT,NULL);
}

