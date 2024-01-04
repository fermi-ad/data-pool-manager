// $Id: LoggedDevice.java,v 1.1 2023/10/04 19:40:29 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;

public interface LoggedDevice
{
	public Node node();
	public String loggerName();
	public String loggedDrf();
	public String event();
	public int id();
}
