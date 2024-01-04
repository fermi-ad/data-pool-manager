// $Id: DeviceDatabaseListObserver.java,v 1.1 2022/11/01 20:40:57 kingc Exp $
package gov.fnal.controls.servers.dpm.events;

import gov.fnal.controls.service.proto.Dbnews;

public interface DeviceDatabaseListObserver
{
	void deviceDatabaseListChange(Dbnews.Request.Info[] dbnews);
}
