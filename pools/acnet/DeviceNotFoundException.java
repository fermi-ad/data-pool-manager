// $Id: DeviceNotFoundException.java,v 1.2 2023/11/01 20:56:57 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

public class DeviceNotFoundException extends AcnetStatusException
{
	DeviceNotFoundException()
	{
		super(AcnetErrors.DBM_NOPROP);
	}
}

