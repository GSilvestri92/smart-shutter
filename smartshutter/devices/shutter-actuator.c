#include "shutter-actuator.h"
#include "presence-sensor.h"
#include "contiki.h"
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include "os/dev/leds.h" 
#define OPEN		10
#define CLOSED		0
process_event_t AUTOMATIC_SHUTTER_EVENT;
extern struct process device_process;
static bool automatic = 0;
static int position = CLOSED;
static int target_lux=500;
void init_shutter()
{
	AUTOMATIC_SHUTTER_EVENT = process_alloc_event();
}

bool isOpen()
{
	return position==OPEN;
}

bool isClosed()
{
	return position==CLOSED;
}

void shutter_up()
{
	if (!isOpen()) position++;
}

void shutter_down()
{
	if (!isClosed()&&!get_obstacle()) position--;
}


int get_position()
{
	return position;
}

bool get_mode()
{
	return automatic;
}

void set_automatic()
{
	automatic = 1;
	leds_single_on(LEDS_YELLOW);
	process_post(&device_process,AUTOMATIC_SHUTTER_EVENT,NULL);
}

void set_manual()
{
	automatic = 0;
	leds_single_off(LEDS_YELLOW);
	process_post(&device_process,AUTOMATIC_SHUTTER_EVENT,NULL);
}

void set_target(int value)
{
	if(value>=100&&value<=1000)	
		target_lux=value;
}


int get_target()
{
	return target_lux;
}

