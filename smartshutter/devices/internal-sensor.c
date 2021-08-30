#include "internal-sensor.h" 
#include "contiki.h"
#include <stdio.h>
#include <string.h>
#include "shutter-actuator.h"
static unsigned int int_lux=0;
static unsigned int out_lux=0;
int int_collect_data()
{
	int pos=get_position();
	int_lux=out_lux*pos/20;	
	return int_lux;
}
void set_out_lux(int i)
{
	out_lux=i;
}
