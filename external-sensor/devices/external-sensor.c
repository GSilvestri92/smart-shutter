#include "external-sensor.h"
#include "contiki.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static unsigned int ext_lux=1500;

int get_ext_lux()
{
	return ext_lux;
}

int ext_collect_data()
{
	int max_readable=10000;
	int pace=200;
	int lower= ((ext_lux-pace)>=0) ? (-pace) : 0;	
	int upper= ((ext_lux+pace)>=max_readable) ? max_readable : pace;	
	int variation=rand()%(upper-lower)+lower;
	ext_lux=ext_lux+variation;
	return ext_lux;
}

