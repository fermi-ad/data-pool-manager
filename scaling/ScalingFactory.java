// $Id: ScalingFactory.java,v 1.4 2023/09/26 20:52:04 kingc Exp $
package gov.fnal.controls.servers.dpm.scaling;

import gov.fnal.controls.service.proto.Lookup_v2;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;

public class ScalingFactory
{
	static final NoScaling noScaling = new NoScaling();

	public static Scaling get(WhatDaq whatDaq)
	{
		final Lookup_v2.DeviceInfo dInfo = whatDaq.dInfo;

        switch (whatDaq.property()) {
         case STATUS:
		 	return DPMBasicStatusScaling.get(whatDaq);

         case CONTROL:
		 	return DPMBasicControlScaling.get(whatDaq);

         case READING:
			if (dInfo.reading != null && dInfo.reading.scaling != null)
		 		return DPMReadSetScaling.get(whatDaq, dInfo.reading.scaling);
			break;

         case SETTING:
			if (dInfo.setting != null)
		 		return DPMReadSetScaling.get(whatDaq, dInfo.setting.scaling);
			break;

         case ANALOG:
		 	if (dInfo.analogAlarm != null && dInfo.reading != null && dInfo.reading.scaling != null)
				return DPMAnalogAlarmScaling.get(whatDaq, dInfo.reading.scaling);

         case DIGITAL:
		 	return DPMDigitalAlarmScaling.get(whatDaq);
        }

		return noScaling; 
	}
}